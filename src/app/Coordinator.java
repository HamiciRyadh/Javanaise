package app;

import jvn.jvnImpl.JvnCoordImpl;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Coordinator {

    public static void main(String[] args) throws RemoteException {
        final JvnCoordImpl coord = JvnCoordImpl.jvnGetCoordinator();
        Registry registry = LocateRegistry.createRegistry(1099);
        registry.rebind("Coordinator", coord);
        System.out.println("The Coordinator is now available.");
    }
}
