package com.geekvplayer.app;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class PlaybackPolicyTest {
    @Test public void retainsMidMoviePosition() { assertEquals(45000, PlaybackPolicy.resumePosition(45000, 90000)); }
    @Test public void completedVideoStartsOver() { assertEquals(0, PlaybackPolicy.resumePosition(90000, 90000)); }
    @Test public void nearEndStartsOver() { assertEquals(0, PlaybackPolicy.resumePosition(88000, 90000)); }
    @Test public void positionOutsideDurationStartsOver() { assertEquals(0, PlaybackPolicy.resumePosition(100000, 90000)); }
    @Test public void negativePositionIsClamped() { assertEquals(0, PlaybackPolicy.resumePosition(-10, 90000)); }
    @Test public void unknownDurationKeepsPosition() { assertEquals(45000, PlaybackPolicy.resumePosition(45000, -1)); }
    @Test public void formatsShortAndLongVideos() {
        assertEquals("0:00", PlaybackPolicy.time(-1));
        assertEquals("1:05", PlaybackPolicy.time(65000));
        assertEquals("1:01:01", PlaybackPolicy.time(3661000));
    }
}
