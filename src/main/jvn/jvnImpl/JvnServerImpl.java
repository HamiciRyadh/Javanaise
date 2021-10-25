/*
 * JAVANAISE Implementation
 * JvnServerImpl class
 * Implementation of a Jvn server
 * Contact: 
 *
 * Authors: 
 */

package main.jvn.jvnImpl;

import main.jvn.JvnLocalServer;
import main.jvn.JvnObject;
import main.jvn.JvnRemoteCoord;
import main.jvn.JvnRemoteServer;
import main.pojo.JvnException;
import main.pojo.Lock;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.io.*;
import java.util.HashMap;
import java.util.Map;


public class JvnServerImpl extends UnicastRemoteObject implements JvnLocalServer, JvnRemoteServer {

    private static final long serialVersionUID = 1L;
    // A JVN server is managed as a singleton
    private static JvnServerImpl js = null;

    private final Map<Integer, JvnObject> jvnObjectMap;
    private final JvnRemoteCoord coordinator;

    /**
     * Default constructor
     *
     * @throws JvnException
     **/
    private JvnServerImpl() throws Exception {
        super();
        // to be completed
        jvnObjectMap = new HashMap<>();
        coordinator = (JvnRemoteCoord) LocateRegistry.getRegistry().lookup("Coordinator");
    }

    /**
     * Static method allowing an application to get a reference to
     * a JVN server instance
     *
     * @throws JvnException
     **/
    public static JvnServerImpl jvnGetServer() {
        if (js == null) {
            try {
                js = new JvnServerImpl();
            } catch (Exception e) {
                return null;
            }
        }
        return js;
    }

    /**
     * The JVN service is not used anymore
     *
     * @throws JvnException
     **/
    public void jvnTerminate() throws JvnException {
        try {
            coordinator.jvnTerminate(this);
        } catch (RemoteException e) {
            e.printStackTrace();
            throw new JvnException("A RemoteException occurred while terminating.");
        }
    }

    @Override
    public JvnObject findCachedValue(int joi) throws JvnException {
        return jvnObjectMap.get(joi);
    }

    /**
     * creation of a JVN object
     *
     * @param o : the JVN object state
     * @throws JvnException
     **/
    public JvnObject jvnCreateObject(Serializable o) throws JvnException {
        try {
            final JvnObject jo = new JvnObjectImpl(coordinator.jvnGetObjectId(), o);
            jvnObjectMap.put(jo.jvnGetObjectId(), jo);
            return jo;
        } catch (RemoteException e) {
            e.printStackTrace();
            throw new JvnException("A RemoteException occurred while requesting a unique ID.");
        }
    }

    /**
     * Associate a symbolic name with a JVN object
     *
     * @param jon : the JVN object name
     * @param jo  : the JVN object
     * @throws JvnException
     **/
    public void jvnRegisterObject(String jon, JvnObject jo) throws JvnException {
        try {
            coordinator.jvnRegisterObject(jon, jo, this);
        } catch (RemoteException e) {
            e.printStackTrace();
            throw new JvnException("A RemoteException occurred while registering the object.");
        }
    }

    /**
     * Provide the reference of a JVN object being given its symbolic name
     *
     * @param jon : the JVN object name
     * @return the JVN object
     * @throws JvnException
     **/
    public JvnObject jvnLookupObject(String jon) throws JvnException {
        try {
            final JvnObject jo = coordinator.jvnLookupObject(jon, this);
            if (jo != null) {
                jvnObjectMap.put(jo.jvnGetObjectId(), jo);
                jo.updateLock(Lock.NO_LOCK);
            }
            return jo;
        } catch (RemoteException e) {
            e.printStackTrace();
            throw new JvnException("A RemoteException occurred while looking for the object.");
        }
    }

    /**
     * Get a Read lock on a JVN object
     *
     * @param joi : the JVN object identification
     * @return the current JVN object state
     * @throws JvnException
     **/
    public Serializable jvnLockRead(int joi) throws JvnException {
        try {
            final JvnObject jo = (JvnObject) coordinator.jvnLockRead(joi, this);
            jvnObjectMap.put(jo.jvnGetObjectId(), jo);
            return jo.jvnGetSharedObject();
        } catch (RemoteException e) {
            e.printStackTrace();
            throw new JvnException("A RemoteException occurred while request a read lock.");
        }
    }

    /**
     * Get a Write lock on a JVN object
     *
     * @param joi : the JVN object identification
     * @return the current JVN object state
     * @throws JvnException
     **/
    public Serializable jvnLockWrite(int joi) throws JvnException {
        try {
            final JvnObject jo = (JvnObject) coordinator.jvnLockWrite(joi, this);
            jvnObjectMap.put(jo.jvnGetObjectId(), jo);
            return jo.jvnGetSharedObject();
        } catch (RemoteException e) {
            e.printStackTrace();
            throw new JvnException("A RemoteException occurred while request a write lock.");
        }
    }

    /**
     * Invalidate the Read lock of the JVN object identified by id
     * called by the JvnCoord
     *
     * @param joi : the JVN object id
     * @return void
     * @throws java.rmi.RemoteException,JvnException
     **/
    public void jvnInvalidateReader(int joi) throws java.rmi.RemoteException, JvnException {
        final JvnObject jo = jvnObjectMap.get(joi);
        if (jo == null) throw new JvnException("JvnObjectId does not exist.");
        jvnObjectMap.put(jo.jvnGetObjectId(), jo);
        jo.jvnInvalidateReader();
    }

    /**
     * Invalidate the Write lock of the JVN object identified by id
     *
     * @param joi : the JVN object id
     * @return the current JVN object state
     * @throws java.rmi.RemoteException,JvnException
     **/
    public Serializable jvnInvalidateWriter(int joi) throws java.rmi.RemoteException, JvnException {
        final JvnObject jo = jvnObjectMap.get(joi);
        if (jo == null) throw new JvnException("JvnObjectId does not exist.");
        jvnObjectMap.put(jo.jvnGetObjectId(), jo);
        return jo.jvnInvalidateWriter();
    }

    /**
     * Reduce the Write lock of the JVN object identified by id
     *
     * @param joi : the JVN object id
     * @return the current JVN object state
     * @throws java.rmi.RemoteException,JvnException
     **/
    public Serializable jvnInvalidateWriterForReader(int joi) throws java.rmi.RemoteException, JvnException {
        final JvnObject jo = jvnObjectMap.get(joi);
        if (jo == null) throw new JvnException("JvnObjectId does not exist.");
        jvnObjectMap.put(jo.jvnGetObjectId(), jo);
        return jo.jvnInvalidateWriterForReader();
    }
}