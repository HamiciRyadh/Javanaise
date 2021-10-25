## What has been done:
- JVN1
- JVN2
- Extension 1: When a client's cache hits a certain size, automatically remove the oldest content.
- Extension 2: When a client is deemed unattainable, remove it from the coordinator's servers' list.
- Extension 3: The coordinator saves the changes to data in a file and, at startup, loads the file before becoming 
available and then informs the clients of its new instance.
- Extension 5: After a transaction is started, the lock is possessed until the end of the transaction.
