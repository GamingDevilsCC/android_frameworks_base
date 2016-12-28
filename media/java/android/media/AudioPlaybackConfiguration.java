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
 */

package android.media;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.os.Binder;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * The AudioPlaybackConfiguration class collects the information describing an audio playback
 * session.
 */
public final class AudioPlaybackConfiguration implements Parcelable {
    private final static String TAG = new String("AudioPlaybackConfiguration");

    /** @hide */
    public final static int PLAYER_PIID_INVALID = -1;

    // information about the implementation
    /**
     * @hide
     * An unknown type of player
     */
    @SystemApi
    public final static int PLAYER_TYPE_UNKNOWN = -1;
    /**
     * @hide
     * Player backed by a java android.media.AudioTrack player
     */
    @SystemApi
    public final static int PLAYER_TYPE_JAM_AUDIOTRACK = 1;
    /**
     * @hide
     * Player backed by a java android.media.MediaPlayer player
     */
    @SystemApi
    public final static int PLAYER_TYPE_JAM_MEDIAPLAYER = 2;
    /**
     * @hide
     * Player backed by a java android.media.SoundPool player
     */
    @SystemApi
    public final static int PLAYER_TYPE_JAM_SOUNDPOOL = 3;
    /**
     * @hide
     * Player backed by a C OpenSL ES AudioPlayer player with a BufferQueue source
     */
    @SystemApi
    public final static int PLAYER_TYPE_SLES_AUDIOPLAYER_BUFFERQUEUE = 11;
    /**
     * @hide
     * Player backed by a C OpenSL ES AudioPlayer player with a URI or FD source
     */
    @SystemApi
    public final static int PLAYER_TYPE_SLES_AUDIOPLAYER_URI_FD = 12;

    /** @hide */
    @IntDef({
        PLAYER_TYPE_UNKNOWN,
        PLAYER_TYPE_JAM_AUDIOTRACK,
        PLAYER_TYPE_JAM_MEDIAPLAYER,
        PLAYER_TYPE_JAM_SOUNDPOOL,
        PLAYER_TYPE_SLES_AUDIOPLAYER_BUFFERQUEUE,
        PLAYER_TYPE_SLES_AUDIOPLAYER_URI_FD,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface PlayerType {}

    /**
     * @hide
     * An unknown player state
     */
    @SystemApi
    public static final int PLAYER_STATE_UNKNOWN = -1;
    /**
     * @hide
     * The resources of the player have been released, it cannot play anymore
     */
    @SystemApi
    public static final int PLAYER_STATE_RELEASED = 0;
    /**
     * @hide
     * The state of a player when it's created
     */
    @SystemApi
    public static final int PLAYER_STATE_IDLE = 1;
    /**
     * @hide
     * The state of a player that is actively playing
     */
    @SystemApi
    public static final int PLAYER_STATE_STARTED = 2;
    /**
     * @hide
     * The state of a player where playback is paused
     */
    @SystemApi
    public static final int PLAYER_STATE_PAUSED = 3;
    /**
     * @hide
     * The state of a player where playback is stopped
     */
    @SystemApi
    public static final int PLAYER_STATE_STOPPED = 4;

    /** @hide */
    @IntDef({
        PLAYER_STATE_UNKNOWN,
        PLAYER_STATE_RELEASED,
        PLAYER_STATE_IDLE,
        PLAYER_STATE_STARTED,
        PLAYER_STATE_PAUSED,
        PLAYER_STATE_STOPPED
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface PlayerState {}

    // immutable data
    private final int mPlayerIId;

    // not final due to anonymization step
    private int mPlayerType;
    private int mClientUid;
    private int mClientPid;

    private int mPlayerState;
    private AudioAttributes mPlayerAttr; // never null

    /**
     * Never use without initializing parameters afterwards
     */
    private AudioPlaybackConfiguration(int piid) {
        mPlayerIId = piid;
    }

    /**
     * @hide
     */
    public AudioPlaybackConfiguration(PlayerBase.PlayerIdCard pic) {
        mPlayerIId = pic.mPIId;
        mPlayerType = pic.mPlayerType;
        mClientUid = pic.mClientUid;
        mClientPid = pic.mClientPid;
        mPlayerState = PLAYER_STATE_IDLE;
        mPlayerAttr = pic.mAttributes;
    }

    // Note that this method is called server side, so no "privileged" information is ever sent
    // to a client that is not supposed to have access to it.
    /**
     * @hide
     * Creates a copy of the playback configuration that is stripped of any data enabling
     * identification of which application it is associated with ("anonymized").
     * @param toSanitize
     */
    public static AudioPlaybackConfiguration anonymizedCopy(AudioPlaybackConfiguration in) {
        final AudioPlaybackConfiguration anonymCopy = new AudioPlaybackConfiguration(in.mPlayerIId);
        anonymCopy.mPlayerState = in.mPlayerState;
        // do not reuse the full attributes: only usage, content type and public flags are allowed
        anonymCopy.mPlayerAttr = new AudioAttributes.Builder()
                .setUsage(in.mPlayerAttr.getUsage())
                .setContentType(in.mPlayerAttr.getContentType())
                .setFlags(in.mPlayerAttr.getFlags())
                .build();
        // anonymized data
        anonymCopy.mPlayerType = PLAYER_TYPE_UNKNOWN;
        anonymCopy.mClientUid = 0;
        anonymCopy.mClientPid = 0;
        return anonymCopy;
    }

    /**
     * Return the {@link AudioAttributes} of the corresponding player.
     * @return the audio attributes of the player
     */
    public AudioAttributes getAudioAttributes() {
        return mPlayerAttr;
    }

    /**
     * @hide
     * Return the uid of the client application that created this player.
     * @return the uid of the client
     */
    @SystemApi
    public int getClientUid() {
        return mClientUid;
    }

    /**
     * @hide
     * Return the pid of the client application that created this player.
     * @return the pid of the client
     */
    @SystemApi
    public int getClientPid() {
        return mClientPid;
    }

    /**
     * @hide
     * Return the type of player linked to this configuration. The return value is one of
     * {@link #PLAYER_TYPE_JAM_AUDIOTRACK}, {@link #PLAYER_TYPE_JAM_MEDIAPLAYER},
     * {@link #PLAYER_TYPE_JAM_SOUNDPOOL}, {@link #PLAYER_TYPE_SLES_AUDIOPLAYER_BUFFERQUEUE},
     * {@link #PLAYER_TYPE_SLES_AUDIOPLAYER_URI_FD}, or {@link #PLAYER_TYPE_UNKNOWN}.
     * @return the type of the player.
     */
    @SystemApi
    public @PlayerType int getPlayerType() {
        return mPlayerType;
    }

    /**
     * @hide
     * Return the current state of the player linked to this configuration. The return value is one
     * of {@link #PLAYER_STATE_IDLE}, {@link #PLAYER_STATE_PAUSED}, {@link #PLAYER_STATE_STARTED},
     * {@link #PLAYER_STATE_STOPPED}, {@link #PLAYER_STATE_RELEASED} or
     * {@link #PLAYER_STATE_UNKNOWN}.
     * @return the state of the player.
     */
    @SystemApi
    public @PlayerState int getPlayerState() {
        return mPlayerState;
    }

    /**
     * @hide
     * Handle a change of audio attributes
     * @param attr
     */
    public boolean handleAudioAttributesEvent(@NonNull AudioAttributes attr) {
        final boolean changed = !attr.equals(mPlayerAttr);
        mPlayerAttr = attr;
        return changed;
    }

    /**
     * @hide
     * Handle a player state change
     * @param event
     * @return true if the state changed, false otherwise
     */
    public boolean handleStateEvent(int event) {
        final boolean changed = (mPlayerState != event);
        mPlayerState = event;
        return changed;
    }

    /**
     * @hide
     * Returns true if the player is considered "active", i.e. actively playing, and thus
     * in a state that should make it considered for the list public (sanitized) active playback
     * configurations
     * @return true if active
     */
    public boolean isActive() {
        switch (mPlayerState) {
            case PLAYER_STATE_STARTED:
                return true;
            case PLAYER_STATE_UNKNOWN:
            case PLAYER_STATE_RELEASED:
            case PLAYER_STATE_IDLE:
            case PLAYER_STATE_PAUSED:
            case PLAYER_STATE_STOPPED:
            default:
                return false;
        }
    }

    /**
     * @hide
     * For AudioService dump
     * @param pw
     */
    public void dump(PrintWriter pw) {
        pw.println("  ID:" + mPlayerIId
                + " -- type:" + toLogFriendlyPlayerType(mPlayerType)
                + " -- u/pid:" + mClientUid +"/" + mClientPid
                + " -- state:" + toLogFriendlyPlayerState(mPlayerState)
                + " -- attr:" + mPlayerAttr);
    }

    public static final Parcelable.Creator<AudioPlaybackConfiguration> CREATOR
            = new Parcelable.Creator<AudioPlaybackConfiguration>() {
        /**
         * Rebuilds an AudioPlaybackConfiguration previously stored with writeToParcel().
         * @param p Parcel object to read the AudioPlaybackConfiguration from
         * @return a new AudioPlaybackConfiguration created from the data in the parcel
         */
        public AudioPlaybackConfiguration createFromParcel(Parcel p) {
            return new AudioPlaybackConfiguration(p);
        }
        public AudioPlaybackConfiguration[] newArray(int size) {
            return new AudioPlaybackConfiguration[size];
        }
    };

    @Override
    public int hashCode() {
        return Objects.hash(mPlayerIId, mPlayerType, mClientUid, mClientPid);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mPlayerIId);
        dest.writeInt(mPlayerType);
        dest.writeInt(mClientUid);
        dest.writeInt(mClientPid);
        dest.writeInt(mPlayerState);
        mPlayerAttr.writeToParcel(dest, 0);
    }

    private AudioPlaybackConfiguration(Parcel in) {
        mPlayerIId = in.readInt();
        mPlayerType = in.readInt();
        mClientUid = in.readInt();
        mClientPid = in.readInt();
        mPlayerState = in.readInt();
        mPlayerAttr = AudioAttributes.CREATOR.createFromParcel(in);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof AudioPlaybackConfiguration)) return false;

        AudioPlaybackConfiguration that = (AudioPlaybackConfiguration) o;

        return ((mPlayerIId == that.mPlayerIId)
                && (mPlayerType == that.mPlayerType)
                && (mClientUid == that.mClientUid)
                && (mClientPid == that.mClientPid));
    }

    //=====================================================================
    // Utilities

    /** @hide */
    public static String toLogFriendlyPlayerType(int type) {
        switch (type) {
            case PLAYER_TYPE_UNKNOWN: return "unknown";
            case PLAYER_TYPE_JAM_AUDIOTRACK: return "android.media.AudioTrack";
            case PLAYER_TYPE_JAM_MEDIAPLAYER: return "android.media.MediaPlayer";
            case PLAYER_TYPE_JAM_SOUNDPOOL:   return "android.media.SoundPool";
            case PLAYER_TYPE_SLES_AUDIOPLAYER_BUFFERQUEUE:
                return "OpenSL ES AudioPlayer (Buffer Queue)";
            case PLAYER_TYPE_SLES_AUDIOPLAYER_URI_FD:
                return "OpenSL ES AudioPlayer (URI/FD)";
            default:
                return "unknown player type - FIXME";
        }
    }

    /** @hide */
    public static String toLogFriendlyPlayerState(int state) {
        switch (state) {
            case PLAYER_STATE_UNKNOWN: return "unknown";
            case PLAYER_STATE_RELEASED: return "released";
            case PLAYER_STATE_IDLE: return "idle";
            case PLAYER_STATE_STARTED: return "started";
            case PLAYER_STATE_PAUSED: return "paused";
            case PLAYER_STATE_STOPPED: return "stopped";
            default:
                return "unknown player state - FIXME";
        }
    }
}