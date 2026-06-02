package org.gradle.cache.internal.locklistener;

import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.util.Optional;
import java.util.Set;

/**
 * Workspace-only Gradle cache patch for sandboxed environments that disallow sockets.
 */
public class DefaultFileLockCommunicator implements FileLockCommunicator {
    public DefaultFileLockCommunicator(InetAddressProvider inetAddressProvider) {
    }

    @Override
    public boolean pingOwner(java.net.InetAddress address, int port, long lockId, String displayName) {
        return false;
    }

    @Override
    public Optional<DatagramPacket> receive() throws java.io.IOException {
        try {
            Thread.sleep(60_000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return Optional.empty();
    }

    @Override
    public FileLockPacketPayload decode(DatagramPacket packet) {
        throw new UnsupportedOperationException("Socket communication is disabled in this workspace build.");
    }

    @Override
    public void confirmUnlockRequest(SocketAddress requester, long lockId) {
    }

    @Override
    public void confirmLockRelease(Set<SocketAddress> requesters, long lockId) {
    }

    @Override
    public void stop() {
    }

    @Override
    public int getPort() {
        return -1;
    }
}
