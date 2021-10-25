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
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class JvnServerImpl extends UnicastRemoteObject implements JvnLocalServer, JvnRemoteServer {

    private static final long serialVersionUID = 1L;
    private static final int CACHE_FLUSH_THRESHOLD = 8;

    // A JVN server is managed as a singleton
    private static JvnServerImpl js = null;

    private final Map<Integer, JvnObject> jvnObjectMap;
    private final Map<Integer, Date> jvnObjectAccessMap;
    private JvnRemoteCoord coordinator;

    /**
     * Default constructor
     *
     * @throws JvnException
     **/
    private JvnServerImpl() throws Exception {
        super();
        // to be completed
        jvnObjectMap = new HashMap<>();
        jvnObjectAccessMap = new HashMap<>();
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
    public void jvnUpdateCoordinator(JvnRemoteCoord coord) throws java.rmi.RemoteException, JvnException {
        this.coordinator = coord;
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
            if (jvnObjectMap.size() > CACHE_FLUSH_THRESHOLD) {
                flushCache();
            }
            jvnObjectMap.put(jo.jvnGetObjectId(), jo);
            jvnObjectAccessMap.put(jo.jvnGetObjectId(), new Date());
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
                if (jvnObjectMap.size() > CACHE_FLUSH_THRESHOLD) {
                    flushCache();
                }
                jvnObjectMap.put(jo.jvnGetObjectId(), jo);
                jvnObjectAccessMap.put(jo.jvnGetObjectId(), new Date());
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
            jvnObjectAccessMap.put(jo.jvnGetObjectId(), new Date());
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
            jvnObjectAccessMap.put(jo.jvnGetObjectId(), new Date());
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
        jvnObjectAccessMap.put(jo.jvnGetObjectId(), new Date());
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
        jvnObjectAccessMap.put(jo.jvnGetObjectId(), new Date());
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
        jvnObjectAccessMap.put(jo.jvnGetObjectId(), new Date());
        return jo.jvnInvalidateWriterForReader();
    }

    private void flushCache() {
        final Map.Entry<Integer, Date> oldest = jvnObjectAccessMap.entrySet().stream().min(Comparator.comparing(Map.Entry::getValue)).get();
        jvnObjectMap.remove(oldest.getKey());
    }
}