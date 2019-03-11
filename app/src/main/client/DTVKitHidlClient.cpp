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
 *  @date     2018/1/12
 *  @par function description:
 *  - 1 share memory hwbinder client
 */

#define LOG_TAG "DTVKitHidlClient"
#include <utils/Log.h>

#include <android/hidl/allocator/1.0/IAllocator.h>
#include <android/hidl/memory/1.0/IMemory.h>
#include <hidlmemory/mapping.h>
#include "DTVKitHidlClient.h"
namespace android {

using ::android::hidl::allocator::V1_0::IAllocator;
using ::android::hidl::memory::V1_0::IMemory;

Mutex DTVKitHidlClient::mLock;

sp<IDTVKitServer> DTVKitHidlClient::getDTVKitService() {
    Mutex::Autolock _l(mLock);

    sp<IDTVKitServer> DtvkitService = IDTVKitServer::tryGetService();
    while (DtvkitService == nullptr) {
         usleep(200*1000);//sleep 200ms
         DtvkitService = IDTVKitServer::tryGetService();
         ALOGE("tryGet IDTVKitServer daemon Service");
    };
    mDeathRecipient = new DTVKitDaemonDeathRecipient(this);
    Return<bool> linked = DtvkitService->linkToDeath(mDeathRecipient, /*cookie*/ 0);
    if (!linked.isOk()) {
        ALOGE("Transaction error in linking to IDTVKitServer daemon service death: %s", linked.description().c_str());
    } else if (!linked) {
        ALOGE("Unable to link to IDTVKitServer daemon service death notifications");
    } else {
        ALOGI("Link to IDTVKitServer daemon service death notification successful");
    }
    return DtvkitService ;
}

DTVKitHidlClient::DTVKitHidlClient(connect_type_t type): mType(type)
{
    mDTVKitServer = getDTVKitService();
    mDTVKitHidlCallback = new DTVKitHidlCallback(this);
    mDTVKitServer->setCallback(mDTVKitHidlCallback, static_cast<ConnectType>(type));
}

DTVKitHidlClient::~DTVKitHidlClient()
{
    disconnect();
}

sp<DTVKitHidlClient> DTVKitHidlClient::connect(connect_type_t type)
{
    return new DTVKitHidlClient(type);
}

void DTVKitHidlClient::reconnect()
{
    ALOGI("share memory client reconnect");
    mDTVKitServer.clear();
    //reconnect to server
    mDTVKitServer = getDTVKitService();
    mDTVKitServer->setCallback(mDTVKitHidlCallback, static_cast<ConnectType>(mType));
}

void DTVKitHidlClient::disconnect()
{
    ALOGD("disconnect");
}

void DTVKitHidlClient::setListener(const sp<DTVKitListener> &listener)
{
    mListener = listener;
}

std::string DTVKitHidlClient::request(const std::string& resource, const std::string& json) {
    std::string result;
    mDTVKitServer->request(resource, json, [&](const std::string& res) {
        result = res;
    });

    return result;
}

Return<void> DTVKitHidlClient::DTVKitHidlCallback::notifyCallback(const DTVKitHidlParcel& hidlParcel) {
    ALOGD("[%s] notifyCallback msgType = %d", __FUNCTION__, hidlParcel.msgType);
    sp<DTVKitListener> listener;
    {
        Mutex::Autolock _l(mLock);
        listener = DtvKitClient->mListener;
    }

    parcel_t parcel;
    parcel.msgType = hidlParcel.msgType;
    for (int i = 0; i < hidlParcel.bodyInt.size(); i++) {
        parcel.bodyInt.push_back(hidlParcel.bodyInt[i]);
    }

    for (int i = 0; i < hidlParcel.bodyString.size(); i++) {
        parcel.bodyString.push_back(hidlParcel.bodyString[i]);
    }

    parcel.mem = hidlParcel.mem;

    if (listener != NULL) {
        listener->notify(parcel);
    }
    return Void();
}

void DTVKitHidlClient::DTVKitDaemonDeathRecipient::serviceDied(uint64_t cookie __unused,
        const ::android::wp<::android::hidl::base::V1_0::IBase>& who __unused)
{
    ALOGE("DTVKit daemon died.");
    Mutex::Autolock _l(mLock);

    usleep(200*1000);//sleep 200ms
    DKClient->reconnect();
}

}


