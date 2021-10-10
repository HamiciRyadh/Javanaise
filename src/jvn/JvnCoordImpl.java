/*
 * JAVANAISE Implementation
 * JvnCoordImpl class
 * This class implements the Javanaise central coordinator
 * Contact:  
 *
 * Authors: 
 */

package jvn;

import jvnImpl.JvnObjectContainer;
import jvnImpl.Lock;

import java.io.Serializable;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;


public class JvnCoordImpl extends UnicastRemoteObject implements JvnRemoteCoord {

    private static final long serialVersionUID = 1L;

    private static JvnCoordImpl coord = null;

    private static int jvnObjectIdCounter = 0;
    private static int remoteServerIdCounter = 0;

    /**
     * Key = The unique ID of the JvnObject.
     * Value = An instance of JvnObjectContainer that represents all there is to know about the given JvnObject (its
     * instance and a Map of RemoteServerId for the key and Lock for the value.
     */
    private final Map<Integer, JvnObjectContainer> jvnObjectContainerMap;
    /**
     * Key = The unique ID of the remote server.
     * Value = An instance of the remote server.
     */
    private final Map<Integer, JvnRemoteServer> remoteServerIDsMap;
    /**
     * Key = The unique object name of a JvnObject.
     * Value = The unique id of a JvnObject.
     */
    private final Map<String, Integer> jvnObjectNameIdMap;


    /**
     * Default constructor
     *
     * @throws JvnException
     **/
    private JvnCoordImpl() throws Exception {
        jvnObjectContainerMap = new HashMap<>();
        remoteServerIDsMap = new HashMap<>();
        jvnObjectNameIdMap = new HashMap<>();
    }

    public static JvnCoordImpl jvnGetCoordinator() {
        if (coord == null) {
            try {
                coord = new JvnCoordImpl();
            } catch (Exception e) {
                return null;
            }
        }
        return coord;
    }

    /**
     * Allocate a NEW JVN object id (usually allocated to a
     * newly created JVN object)
     *
     * @throws java.rmi.RemoteException,JvnException
     **/
    public int jvnGetObjectId() throws java.rmi.RemoteException, jvn.JvnException {
        return jvnObjectIdCounter++;
    }

    /**
     * Associate a symbolic name with a JVN object
     *
     * @param jon : the JVN object name
     * @param jo  : the JVN object
     * @param js  : the remote reference of the JVNServer
     * @throws java.rmi.RemoteException,JvnException
     **/
    public void jvnRegisterObject(String jon, JvnObject jo, JvnRemoteServer js) throws java.rmi.RemoteException, jvn.JvnException {
        // Step 1: Check if the remote server is already known
        Integer jsId = findRemoteServerId(js);
        if (jsId == null) {
            // This is the first interaction with this remote server, we have to give it a unique ID and add keep a
            // reference to it.
            jsId = remoteServerIdCounter++;
            remoteServerIDsMap.put(jsId, js);
        }

        // Step 2: Check if the object name is already taken.
        if (jvnObjectNameIdMap.get(jon) != null) {
            throw new JvnException("The requested object name is already taken.");
        }

        // Step 3: Register the JvnObject with the given name.
        jvnObjectNameIdMap.put(jon, jo.jvnGetObjectId());
        final JvnObjectContainer container = new JvnObjectContainer(jo, new HashMap<>());
        container.getRemoteServerLocks().put(jsId, Lock.DEFAULT_REGISTRATION_LOCK);
        jvnObjectContainerMap.put(jo.jvnGetObjectId(), container);
    }

    /**
     * Get the reference of a JVN object managed by a given JVN server
     *
     * @param jon : the JVN object name
     * @param js  : the remote reference of the JVNServer
     * @throws java.rmi.RemoteException,JvnException
     **/
    public JvnObject jvnLookupObject(String jon, JvnRemoteServer js) throws java.rmi.RemoteException, jvn.JvnException {
        // FIXME: What to do with the remote server reference ?
        final Integer joId = jvnObjectNameIdMap.get(jon);
        if (joId == null) {
            // The given object name isn't associated with any known Object.
            return null;
        }

        final JvnObjectContainer joc = jvnObjectContainerMap.get(joId);
        if (joc == null) {
            throw new JvnException("Inconsistency error: Object name defined but no associated object found.");
        }
        return joc.getJvnObject();
    }

    /**
     * Get a Read lock on a JVN object managed by a given JVN server
     *
     * @param joi : the JVN object identification
     * @param js  : the remote reference of the server
     * @return the current JVN object state
     * @throws java.rmi.RemoteException, JvnException
     **/
    public Serializable jvnLockRead(int joi, JvnRemoteServer js) throws java.rmi.RemoteException, JvnException {
        // Check who owns the lock, if no one then give the lock, if someone has the lock then if it is a read lock
        // then give the lock and if it's a write lock invalidate and then give the lock
        final JvnObjectContainer joc = jvnObjectContainerMap.get(joi);
        if (joc == null) {
            throw new JvnException("Object not found, cannot lock.");
        }

        final Integer jsId = findRemoteServerId(js);
        if (jsId == null) {
            throw new JvnException("Remote server not found, cannot lock.");
        }

        // Step 1: Invalidate the existing lock, there should only be one "Write" lock, so when we encounter it we
        // can leave.
        for (Map.Entry<Integer, Lock> entry: joc.getRemoteServerLocks().entrySet()) {
            if (entry.getValue() == Lock.WRITE) {
                remoteServerIDsMap.get(entry.getKey()).jvnInvalidateWriterForReader(joi);
                entry.setValue(Lock.NO_LOCK);
                break;
            }
        }

        // Step 2: Give the requested lock.
//        FIXME Check if we have to change the lock here
//        joc.getJvnObject().jvnLockRead();
        joc.getRemoteServerLocks().put(jsId, Lock.READ);

        return joc.getJvnObject();
    }

    /**
     * Get a Write lock on a JVN object managed by a given JVN server
     *
     * @param joi : the JVN object identification
     * @param js  : the remote reference of the server
     * @return the current JVN object state
     * @throws java.rmi.RemoteException, JvnException
     **/
    public Serializable jvnLockWrite(int joi, JvnRemoteServer js) throws java.rmi.RemoteException, JvnException {
        // Check who owns the lock, if no one then give the lock, if someone has the lock then invalidate and then give
        // the lock
        final JvnObjectContainer joc = jvnObjectContainerMap.get(joi);
        if (joc == null) {
            throw new JvnException("Object not found, cannot lock.");
        }

        final Integer jsId = findRemoteServerId(js);
        if (jsId == null) {
            throw new JvnException("Remote server not found, cannot lock.");
        }

        // Step 1: Invalidate the existing locks.
        for (Map.Entry<Integer, Lock> entry: joc.getRemoteServerLocks().entrySet()) {
            if (entry.getValue() == Lock.NO_LOCK) continue;

            if (entry.getValue() == Lock.READ) {
                remoteServerIDsMap.get(entry.getKey()).jvnInvalidateReader(joi);
            } else { // Lock.WRITE
                remoteServerIDsMap.get(entry.getKey()).jvnInvalidateWriter(joi);
            }
            entry.setValue(Lock.NO_LOCK);
        }

        // Step 2: Give the requested lock.
//        FIXME Check if we have to change the lock here
//        joc.getJvnObject().jvnLockWrite();
        joc.getRemoteServerLocks().put(jsId, Lock.WRITE);

        return joc.getJvnObject();
    }

    /**
     * A JVN server terminates
     *
     * @param js : the remote reference of the server
     * @throws java.rmi.RemoteException, JvnException
     **/
    public void jvnTerminate(JvnRemoteServer js) throws java.rmi.RemoteException, JvnException {
        Integer jsId = findRemoteServerId(js);
        if (jsId == null) {
            throw new JvnException("Remote server not found, cannot terminate.");
        }

        // Step 1: Removing the locks owned by the terminating server.
        jvnObjectContainerMap.values().forEach(joc -> joc.getRemoteServerLocks().remove(jsId));

        // Step 2: Removing from the servers' map.
        remoteServerIDsMap.remove(jsId);
    }

    private Integer findRemoteServerId(JvnRemoteServer js) {
        return remoteServerIDsMap.entrySet().stream()
                .filter(entry -> entry.getValue().equals(js))
                .map(Map.Entry::getKey)
                .findAny()
                .orElse(null);
    }
}

 
