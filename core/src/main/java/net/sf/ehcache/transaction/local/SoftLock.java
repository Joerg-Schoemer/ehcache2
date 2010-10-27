package net.sf.ehcache.transaction.local;

import net.sf.ehcache.Element;
import net.sf.ehcache.transaction.TransactionID;

/**
 * @author Ludovic Orban
 */
public interface SoftLock {

    Object getKey();

    Element getElement();

    Element getOldElement();

    Element getNewElement();

    void setNewElement(Element newElement);

    TransactionID getTransactionID();

    void lock();

    boolean tryLock(long ms) throws InterruptedException;

    void unlock();

    boolean isLocked();

    void freeze();

    void unfreeze();

    boolean isExpired();
}
