package project4;
// Task interface, all task job should implement it
public interface Task<T> 
{
    T execute();
    void init(String init_str);
}
