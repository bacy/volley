package com.android.volley.toolbox;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.android.volley.toolbox.multipart.MultipartEntity;

/**
 * 带进度监听的上传Entity
 * UploadMultipartEntity
 * chenbo
 * @version 3.6
 */
public class UploadMultipartEntity extends MultipartEntity {

    private ProgressListener listener;
    private long offset;

    public void setListener(ProgressListener listener) {
        this.listener = listener;
    }

    @Override
    public void writeTo(OutputStream outstream) throws IOException {
        if (listener == null) {
            super.writeTo(outstream);
        } else {
            super.writeTo(new CountingOutputStream(outstream, offset, this.listener));
        }
    }
    
    static class CountingOutputStream  extends FilterOutputStream {
        private final ProgressListener listener;
        private long transferred;
        private long offset;

        public CountingOutputStream(final OutputStream out, long offset, final ProgressListener listener) {
            super(out);
            this.listener = listener;
            this.transferred = 0;
            this.offset = offset;
        }

        public void write(byte[] b, int off, int len) throws IOException {
            out.write(b, off, len);
            this.transferred += len;
            this.listener.transferred(this.transferred + offset);
        }

        public void write(int b) throws IOException {
            out.write(b);
            this.transferred++;
            this.listener.transferred(this.transferred + offset);
        }
        
        @Override
        public void write(byte[] buffer) throws IOException {
            out.write(buffer);
            this.transferred += buffer.length;
            this.listener.transferred(this.transferred + offset);
        }
        
    }

    public static interface ProgressListener {
        void transferred(long num);
    }
}
