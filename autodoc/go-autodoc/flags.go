package main

import (
    "strings"
)

// parseInfo turns a flag value like
//   title="My API",version="2.1.0"
// into
//   map[string]string{"title":"My API","version":"2.1.0"}
func parseInfo(infoFlag string) map[string]string {
    info := make(map[string]string)
    for _, part := range strings.Split(infoFlag, ",") {
        kv := strings.SplitN(part, "=", 2)
        if len(kv) != 2 {
            continue
        }
        key := strings.TrimSpace(kv[0])
        val := strings.Trim(strings.TrimSpace(kv[1]), `"`)
        info[key] = val
    }
    return info
}

// parseServers turns a flag like
//   url="https://api.example.com",description="Prod";url="https://staging",description="Staging"
// into
//   []map[string]string{
//     {"url":"https://api.example.com","description":"Prod"},
//     {"url":"https://staging","description":"Staging"},
//   }
func parseServers(serversFlag string) []map[string]string {
    var out []map[string]string
    for _, srvSpec := range strings.Split(serversFlag, ";") {
        srvSpec = strings.TrimSpace(srvSpec)
        if srvSpec == "" {
            continue
        }
        srv := make(map[string]string)
        for _, part := range strings.Split(srvSpec, ",") {
            kv := strings.SplitN(part, "=", 2)
            if len(kv) != 2 {
                continue
            }
            key := strings.TrimSpace(kv[0])
            val := strings.Trim(strings.TrimSpace(kv[1]), `"`)
            srv[key] = val
        }
        out = append(out, srv)
    }
    return out
}
