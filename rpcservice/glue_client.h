#ifndef GULE_CLIENT_H
#define GULE_CLIENT_H

#include <iostream>
#include <gui/IGraphicBufferProducer.h>
#include <gui/Surface.h>
#include <android/native_window.h>
#include <GLES2/gl2.h>

#define NUM_BUFFS 2
#define NUM_TEXTURES 3
#define NUM_FBUFFS 1

typedef void (*SIGNAL_CB)(const std::string &signal, const std::string &data);
// Singleton mode, only used by getInstance
class Glue_client {

private:
    Glue_client();
    static Glue_client *p_client;

public:
    int setSignalCallback(SIGNAL_CB cb);
    int RegisterRWSysfsCallback(void *readCb, void *writeCb);
    int UnRegisterRWSysfsCallback(void);
    int RegisterRWPropCallback(void *readCb, void *writeCb);
    int UnRegisterRWPropCallback(void);
    int addInterface(void);
    int SetSurface(int path, void *surface);
    std::string request(const std::string &resource, const std::string &json);
    void setIGraphicBufferProducer(android::sp<android::IGraphicBufferProducer>& bufferProducer);
    static Glue_client* getInstance()
    {
        return p_client;
    }
    void dispatchDraw(int32_t src_width, int32_t src_height, int32_t dst_x, int32_t dst_y, int32_t dst_width, int32_t dst_height, const uint8_t *data);
    void dispatchSignal(const std::string &signal, const std::string &data);
    void dispatchOverlayCreate(int id, uint32_t width, uint32_t height);
    void OverlayDisplay();
    void OverlayUpdate(int id, int32_t dst_x, int32_t dst_y, int32_t dst_width, int32_t dst_height, const uint8_t *data);

private:
    SIGNAL_CB signal_callback = NULL;
    android::sp<android::Surface> m_overlayproducersurface;
    ANativeWindow_Buffer m_buffer;
    ANativeWindow *m_window;
    uint32_t m_screen_width;
    uint32_t m_screen_height;
    EGLDisplay m_display;
    EGLContext m_eglContext;
    EGLSurface m_eglSurface;
    GLuint m_shaderProgram;
    GLint m_texImgLocation;
    GLuint m_bufferObjects[NUM_BUFFS];
    GLuint m_textures[NUM_TEXTURES];
    GLuint m_framebuffers[NUM_FBUFFS];
    GLboolean m_texture_visible[NUM_TEXTURES];

    void initEGL(void);
    void initTextures(void);
    void initTexture(int id, uint32_t width, uint32_t height);
    void initDrawing(void);
};
// no need add lock for new client.we new client first
Glue_client* Glue_client::p_client = new Glue_client();

#endif //GULE_CLIENT_H