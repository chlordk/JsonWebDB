# Query Management

Most applications allow clients to query databases and subsequently scroll/page through the results.
Queries and, subsequently, paging often account for most of the total load on the underlying databases. Therefore, handling these operations efficiently is crucial for performance and scaling.

When scrolling through a resultset, there are two strategies.

1. You can either use database cursors.
2. Or you can re-execute the query and skip down to the rows of interest.

JsonDB supports both methods but favors using database cursors since they are by far the fastest and least demanding on the database.

The downside to this approach is, among others:

1. It requires sessions to hold on to their connections and cursors.
2. Clients must be routed to the same instance of JsonDB at every request.

JsonDB will always hold state. But, depending on the client's behavior, the state might be released again. However, it is always possible to resume a session and restore the state (possibly in another instance in the cluster). The following describes the lifecycle:

1. A client connects, creating a session (state object).

2. Subsequent queries add state (cursors) to the session.

3. When a client is considered inactive, all resources will be released. However, the session can be resumed by any JsonDB instance in the cluster.

4. If the number of sessions (per instance) exceeds the target, a clean-up process will release some sessions, including resources.

5. If the number of cursors (per instance) exceeds the target, a clean-up process will release some cursors.

6. If a client fails to keep the session alive, it will be disconnected, and all resources will be freed up.
