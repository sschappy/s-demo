import com.example.counter.*;

public class Main {
    static {
        try {
            // 加载本地库
            System.loadLibrary("counter_jni");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("无法加载本地库: " + e.getMessage());
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        // 通过 SWIG 生成的 example 类调用 C++ 函数
        String message = counter.getGreeting();
        int sum = counter.add(5, 3);

        System.out.println("Message: " + message);
        System.out.println("Sum of 5 and 3: " + sum);


        System.out.println("测试Counter类...");
        
        // // 创建Counter实例
        // Counter counter = new Counter();
        
        // // 测试基本操作
        // System.out.println("初始计数操作:");
        
        // counter.addOne();
        // System.out.println("执行 addOne()");
        
        // counter.subOne();
        // System.out.println("执行 subOne()");
        
        // counter.add(5);
        // System.out.println("执行 add(5)");
        
        // counter.sub(3);
        // System.out.println("执行 sub(3)");
        
        // System.out.println("所有操作完成！");
        
        // 注意：由于Counter类的私有成员i没有getter方法，
        // 我们无法直接获取计数值，但可以确认方法调用成功
    }
}