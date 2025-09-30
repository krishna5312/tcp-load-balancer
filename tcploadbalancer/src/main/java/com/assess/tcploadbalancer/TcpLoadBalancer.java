package com.assess.tcploadbalancer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.assess.tcploadbalancer.backend.Backend;
import com.assess.tcploadbalancer.backend.BackendStatus;

public class TcpLoadBalancer {

	private final int listenPort;
	private final List<BackendStatus> backends;
	private final ExecutorService executor = Executors.newCachedThreadPool();
	private final AtomicInteger rrIndex = new AtomicInteger(0);

	public TcpLoadBalancer(int listenPort, List<BackendStatus> backends) {
		this.listenPort = listenPort;
		this.backends = backends;
	}

	public void start() {
		try (ServerSocket serverSocket = new ServerSocket(listenPort)) {
			System.out.println("TCP Load Balancer listening on port " + serverSocket.getLocalPort());
			while (true) {
				Socket clientSocket = serverSocket.accept();
				executor.submit(() -> handleClient(clientSocket));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void handleClient(Socket clientSocket) {
		try {
			Backend backend = chooseBackend();
			if (backend == null) {
				// No alive backends available
				PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
				out.println("503 Service Unavailable - no backend alive");
				clientSocket.close();
				return;
			}

			Socket backendSocket = new Socket(backend.getHost(), backend.getPort());
			executor.submit(() -> forwardTraffic(clientSocket, backendSocket));
			executor.submit(() -> forwardTraffic(backendSocket, clientSocket));
		} catch (IOException e) {
			e.printStackTrace();
			try {
				clientSocket.close();
			} catch (IOException ignored) {
			}

		}
	}

	private void forwardTraffic(Socket inputSocket, Socket outputSocket) {
		try (InputStream in = inputSocket.getInputStream(); OutputStream out = outputSocket.getOutputStream()) {
			byte[] buffer = new byte[8192];
			int read;
			while ((read = in.read(buffer)) != -1) {
				out.write(buffer, 0, read);
				out.flush();
			}
		} catch (IOException ignored) {
			// connection closed, stop forwarding
		} finally {
			try {
				inputSocket.close();
			} catch (IOException ignored) {
			}
			try {
				outputSocket.close();
			} catch (IOException ignored) {
			}
		}
	}

	/**
	 * Round-robin backend selection that skips DOWN backends.
	 */
	private Backend chooseBackend() {
		if (backends.isEmpty())
			return null;

		int attempts = 0;
		while (attempts < backends.size()) {
			int idx = Math.floorMod(rrIndex.getAndIncrement(), backends.size());
			BackendStatus status = backends.get(idx);
			if (status.isAlive()) {
				return status.getBackend();
			}
			attempts++;
		}
		return null; // all backends down
	}
}
