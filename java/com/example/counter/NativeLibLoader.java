package com.example.counter;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class NativeLibLoader {
    private static final String NATIVE_LIB_DIR = "nativeLib";
    private static final Set<String> loadedLibraries = new HashSet<>();
    private static File extractedLibsDir = null;
    
    static {
        // 在类加载时直接加载本地库
        loadNativeLibraries();
    }
    
    private static synchronized void loadNativeLibraries() {
        try {
            // 提取本地库到临时目录
            extractNativeLibraries();
            
            // 直接加载 DLL 文件
            loadLibrariesDirect();
            
        } catch (Exception e) {
            System.err.println("Failed to load native libraries: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Native library loading failed", e);
        }
    }
    
    private static void extractNativeLibraries() throws IOException {
        if (extractedLibsDir != null) {
            return; // 已经提取过了
        }
        
        // 创建临时目录
        Path tempDir = Files.createTempDirectory("counter_native_libs");
        extractedLibsDir = tempDir.toFile();
        extractedLibsDir.deleteOnExit();
        
        System.out.println("Created temporary directory: " + tempDir.toAbsolutePath());
        
        // 获取 JAR 中的本地库文件
        ClassLoader classLoader = NativeLibLoader.class.getClassLoader();
        
        // 只需要加载动态库（.dll 文件）
        String[] dynamicLibraries = {"counter_jni"};
        
        boolean foundAnyLibrary = false;
        for (String libName : dynamicLibraries) {
            String systemLibName = System.mapLibraryName(libName);
            String resourcePath = NATIVE_LIB_DIR + "/" + systemLibName;
            
            System.out.println("Looking for resource: " + resourcePath);
            
            InputStream is = classLoader.getResourceAsStream(resourcePath);
            if (is != null) {
                Path libFile = tempDir.resolve(systemLibName);
                Files.copy(is, libFile, StandardCopyOption.REPLACE_EXISTING);
                libFile.toFile().deleteOnExit();
                is.close();
                
                // 验证文件确实被创建了
                if (Files.exists(libFile)) {
                    long fileSize = Files.size(libFile);
                    System.out.println("Successfully extracted native library: " + systemLibName + 
                                     " (size: " + fileSize + " bytes)");
                    foundAnyLibrary = true;
                } else {
                    System.err.println("ERROR: File was not created: " + libFile);
                }
            } else {
                System.err.println("ERROR: Could not find native library resource: " + resourcePath);
            }
        }
        
        if (!foundAnyLibrary) {
            throw new IOException("No native libraries were found in the JAR!");
        }
    }
    
    private static void loadLibrariesDirect() {
        // 直接使用 System.load() 加载 DLL 文件
        if (extractedLibsDir == null || !extractedLibsDir.exists()) {
            throw new RuntimeException("Extracted libraries directory not available");
        }
        
        String[] librariesToLoad = {"counter_jni"};
        
        for (String libName : librariesToLoad) {
            if (!loadedLibraries.contains(libName)) {
                String systemLibName = System.mapLibraryName(libName);
                File libFile = new File(extractedLibsDir, systemLibName);
                
                if (libFile.exists()) {
                    try {
                        System.out.println("Loading library directly from: " + libFile.getAbsolutePath());
                        System.load(libFile.getAbsolutePath());
                        loadedLibraries.add(libName);
                        System.out.println("Successfully loaded library: " + libName);
                    } catch (UnsatisfiedLinkError e) {
                        System.err.println("Failed to load library: " + libName);
                        System.err.println("Error: " + e.getMessage());
                        
                        // 检查 DLL 依赖关系
                        checkDllDependencies(libFile);
                        throw e;
                    }
                } else {
                    throw new RuntimeException("Library file not found: " + libFile.getAbsolutePath());
                }
            }
        }
    }
    
    private static void checkDllDependencies(File dllFile) {
        System.err.println("=== Checking DLL Dependencies ===");
        System.err.println("DLL: " + dllFile.getAbsolutePath());
        System.err.println("File exists: " + dllFile.exists());
        System.err.println("File size: " + dllFile.length() + " bytes");
        
        // 在 Windows 上，我们可以尝试使用 dumpbin 来检查依赖
        try {
            ProcessBuilder pb = new ProcessBuilder("dumpbin", "/dependents", dllFile.getAbsolutePath());
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            System.err.println("Dependencies:");
            while ((line = reader.readLine()) != null) {
                if (line.trim().endsWith(".dll")) {
                    System.err.println("  " + line.trim());
                }
            }
            process.waitFor();
        } catch (Exception e) {
            System.err.println("Could not run dumpbin: " + e.getMessage());
        }
    }
    
    // 这个方法现在只用于向后兼容，实际加载在静态块中完成
    public static void loadLibraries() {
        // 已经在静态块中加载了，这里不需要做任何事情
        System.out.println("Native libraries already loaded during class initialization");
    }
}