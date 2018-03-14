// IDTVKit.aidl
package org.dtvkit;

import org.dtvkit.ISignalHandler;

interface IDTVKit {
    String request(String method, String json);
    void registerSignalHandler(ISignalHandler handler);
}
