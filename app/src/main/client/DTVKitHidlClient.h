/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *  @author   junchao.yuan
 *  @version  1.0
 *  @date     2019/3/29
 *  @par function description:
 *  - 1 dtvkit hidl client
 */

#ifndef _DTVKIT_HIDL_CLIENT_H_
#define _DTVKIT_HIDL_CLIENT_H_
#include <utils/RefBase.h>
#include <utils/Mutex.h>

#include <vendor/amlogic/hardware/dtvkitserver/1.0/IDTVKitServer.h>

namespace android {

using ::vendor::amlogic::hardware::dtvkitserver::V1_0::IDTVKitServer;
using ::vendor::amlogic::hardware::dtvkitserver::V1_0::IDTVKitServerCallback;
using ::vendor::amlogic::hardware::dtvkitserver::V1_0::DTVKitHidlParcel;
using ::vendor::amlogic::hardware::dtvkitserver::V1_0::ConnectType;
using ::android::hardware::hidl_array;
using ::android::hardware::hidl_string;
using ::android::hardware::hidl_memory;
using ::android::hardware::hidl_vec;
using ::android::hardware::Return;
using ::android::hardware::Void;
using ::android::sp;

typedef enum {
    CONNECT_TYPE_HAL            = 0,
    CONNECT_TYPE_EXTEND         = 1
} connect_type_t;

typedef struct parcel_s {
    int msgType;
    std::vector<int> bodyInt;
    std::vector<std::string> bodyString;
    hidl_memory mem;
} parcel_t;

class DTVKitListener : virtual public RefBase {
public:
    virtual void notify(const parcel_t &parcel) = 0;
};

class DTVKitHidlClient : virtual public RefBase {
public:
    static sp<DTVKitHidlClient> connect(connect_type_t type);
    DTVKitHidlClient(connect_type_t type);
    ~DTVKitHidlClient();

    void reconnect();
    void disconnect();
    void setListener(const sp<DTVKitListener> &listener);
    std::string request(const std::string& resource, const std::string& json);

private:
    class DTVKitHidlCallback : public IDTVKitServerCallback {
    public:
        DTVKitHidlCallback(DTVKitHidlClient *client): DtvKitClient(client) {};
        Return<void> notifyCallback(const DTVKitHidlParcel& parcel) override;

    private:
        DTVKitHidlClient *DtvKitClient;
    };

    struct DTVKitDaemonDeathRecipient : public android::hardware::hidl_death_recipient  {
        DTVKitDaemonDeathRecipient(DTVKitHidlClient *client): DKClient(client) {};

        // hidl_death_recipient interface
        virtual void serviceDied(uint64_t cookie,
            const ::android::wp<::android::hidl::base::V1_0::IBase>& who) override;
    private:
        DTVKitHidlClient *DKClient;
    };
    sp<DTVKitDaemonDeathRecipient> mDeathRecipient = nullptr;
    sp<IDTVKitServer> getDTVKitService();
    connect_type_t mType;

    static Mutex mLock;
    sp<DTVKitListener> mListener;
    sp<IDTVKitServer> mDTVKitServer;
    sp<DTVKitHidlCallback> mDTVKitHidlCallback = nullptr;
};
}

#endif

