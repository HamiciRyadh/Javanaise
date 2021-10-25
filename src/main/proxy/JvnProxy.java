package main.proxy;

import main.jvn.JvnObject;
import main.jvn.jvnImpl.JvnServerImpl;
import main.pojo.JvnException;
import main.pojo.Sentence;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class JvnProxy implements InvocationHandler {

    private final JvnObject jo;

    private JvnProxy(JvnObject jo) {
        this.jo = jo;
    }


    public static Object newInstance(String jon) throws JvnException {
        JvnObject jo = null;

        try{
            JvnServerImpl js = JvnServerImpl.jvnGetServer();

            // look up the IRC object in the JVN server
            // if not found, create it, and register it in the JVN server
            jo = js.jvnLookupObject(jon);

            if (jo == null) {
                System.out.println(jon + " not found, creating it ...");
                jo = js.jvnCreateObject(new Sentence());
                // after creation, we have a "write" lock on the object
                jo.jvnUnLock();
                js.jvnRegisterObject(jon, jo);
            } else {
                System.out.println(jon + " found.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return JvnProxy.newInstance(jo);
    }

    private static Object newInstance(JvnObject obj) throws JvnException {
        return java.lang.reflect.Proxy.newProxyInstance(
                obj.jvnGetSharedObject().getClass().getClassLoader(),
                obj.jvnGetSharedObject().getClass().getInterfaces(),
                new JvnProxy(obj));
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object result = null;
        try {
            if(method.isAnnotationPresent(Lock.class)) {
                Lock lock = method.getAnnotation(Lock.class);
                switch (lock.type()) {
                    case READ: {
                        jo.jvnLockRead();
                        result = method.invoke(jo.jvnGetSharedObject(), args);
                        break;
                    }
                    case WRITE: {
                        jo.jvnLockWrite();
                        result = method.invoke(jo.jvnGetSharedObject(), args);
                        break;
                    }
                }
                // No matter what type of lock was requested, always unlock at the end.
                jo.jvnUnLock();
            } else {
                // If the method was not annotated, execute it normally.
                result = method.invoke(jo.jvnGetSharedObject(), args);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new JvnException("ProxyException.");
        }
        return result;
    }
}