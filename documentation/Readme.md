# Best performance

The best performance is achieved when reusing cursors. However this requires that a session holds on to it's connection and cursors.

This approach uses a lot of resources and limits the number of sessions considerable.

1. If a client fails to keep the session alive, the session will be disconnected and all resources will be freed up.

2. When a client is considered "inactive" the all resources will be released.

3. If the number of sessions exceeds the target, a clean up process will release some sessions including resources.

4. If the number of cursors exceeds the target, a clean up process will release some cursors.
