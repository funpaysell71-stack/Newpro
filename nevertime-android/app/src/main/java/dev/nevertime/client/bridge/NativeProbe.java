package dev.nevertime.client.bridge;

final class NativeProbe {
    private NativeProbe() {}
    static native String[] snapshot();
}
