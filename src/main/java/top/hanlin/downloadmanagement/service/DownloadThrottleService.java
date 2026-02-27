package top.hanlin.downloadmanagement.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@Service
public class DownloadThrottleService {

	@Autowired
	private IpControlService ipControlService;

	public InputStream throttleInputStream(InputStream in, String ip) throws IOException {
		int rateLimitKB = ipControlService.getDownloadRateLimitKB(ip);
		if (rateLimitKB == Integer.MAX_VALUE) {
			return in;
		}
		return new ThrottledInputStream(in, rateLimitKB);
	}

	private static class ThrottledInputStream extends InputStream {
		private final InputStream delegate;
		private final int rateLimitKB;
		private long startTime;
		private long bytesRead;

		public ThrottledInputStream(InputStream delegate, int rateLimitKB) {
			this.delegate = delegate;
			this.rateLimitKB = rateLimitKB;
			this.startTime = System.currentTimeMillis();
			this.bytesRead = 0;
		}

		@Override
		public int read() throws IOException {
			throttle(1);
			return delegate.read();
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			int read = delegate.read(b, off, len);
			if (read > 0) {
				throttle(read);
			}
			return read;
		}

		private void throttle(int bytes) throws IOException {
			bytesRead += bytes;
			long elapsed = System.currentTimeMillis() - startTime;
			long expectedTime = (bytesRead * 1000L) / (rateLimitKB * 1024L);
			if (elapsed < expectedTime) {
				try {
					Thread.sleep(expectedTime - elapsed);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new IOException("Interrupted", e);
				}
			}
		}

		@Override public void close() throws IOException { delegate.close(); }
		@Override public int available() throws IOException { return delegate.available(); }
		@Override public long skip(long n) throws IOException { return delegate.skip(n); }
		@Override public synchronized void mark(int readlimit) { delegate.mark(readlimit); }
		@Override public synchronized void reset() throws IOException { delegate.reset(); }
		@Override public boolean markSupported() { return delegate.markSupported(); }
	}
}

