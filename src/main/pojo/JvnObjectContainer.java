package main.pojo;

import main.jvn.JvnObject;

import java.io.Serializable;
import java.util.Map;

public class JvnObjectContainer implements Serializable {

    private final JvnObject jvnObject;
    private final Map<Integer, Lock> remoteServerLocks;

    public JvnObjectContainer(JvnObject jvnObject, Map<Integer, Lock> remoteServerLocks) {
        this.jvnObject = jvnObject;
        this.remoteServerLocks = remoteServerLocks;
    }

    public JvnObject getJvnObject() {
        return jvnObject;
    }

    public Map<Integer, Lock> getRemoteServerLocks() {
        return remoteServerLocks;
    }
}
