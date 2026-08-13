package com.bakeflow.integration.application;

import java.time.Duration;import java.time.Instant;

public final class SimpleCircuitBreaker {
    public enum State { CLOSED, OPEN, HALF_OPEN }
    private final int threshold;private final Duration openDuration;private int failures;private Instant openedAt;private boolean probe;
    public SimpleCircuitBreaker(int threshold,Duration openDuration){this.threshold=threshold;this.openDuration=openDuration;}
    public synchronized boolean allow(){if(openedAt==null)return true;if(Instant.now().isBefore(openedAt.plus(openDuration)))return false;if(probe)return false;probe=true;return true;}
    public synchronized void success(){failures=0;openedAt=null;probe=false;}
    public synchronized void failure(){probe=false;if(++failures>=threshold)openedAt=Instant.now();}
    public synchronized State state(){if(openedAt==null)return State.CLOSED;return Instant.now().isBefore(openedAt.plus(openDuration))?State.OPEN:State.HALF_OPEN;}
}
