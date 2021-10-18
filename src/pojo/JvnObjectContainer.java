package pojo;

import jvn.JvnObject;

import java.util.Map;

public class JvnObjectContainer {

    private JvnObject jvnObject;
    private final Map<Integer, Lock> remoteServerLocks;

    public JvnObjectContainer(JvnObject jvnObject, Map<Integer, Lock> remoteServerLocks) {
        this.jvnObject = jvnObject;
        this.remoteServerLocks = remoteServerLocks;
    }

    public JvnObject getJvnObject() {
        return jvnObject;
    }

    public void setJvnObject(JvnObject jvnObject) {
        this.jvnObject = jvnObject;
    }

    public Map<Integer, Lock> getRemoteServerLocks() {
        return remoteServerLocks;
    }
}
