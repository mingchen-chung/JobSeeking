package project4;

import java.rmi.Remote;
import java.rmi.RemoteException;
// compute interface, compute engine should implement it
public interface Compute extends Remote 
{
    <T>T executeTask(Task<T> t, String target) throws RemoteException;
}