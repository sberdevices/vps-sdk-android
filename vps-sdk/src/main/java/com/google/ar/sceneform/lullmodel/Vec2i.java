// automatically generated by the FlatBuffers compiler, do not modify

package com.google.ar.sceneform.lullmodel;

import com.google.flatbuffers.FlatBufferBuilder;
import com.google.flatbuffers.Struct;

import java.nio.ByteBuffer;

@SuppressWarnings("unused")
public final class Vec2i extends Struct {
    public static int createVec2i(FlatBufferBuilder builder, int x, int y) {
        builder.prep(4, 8);
        builder.putInt(y);
        builder.putInt(x);
        return builder.offset();
    }

    public void __init(int _i, ByteBuffer _bb) {
        bb_pos = _i;
        bb = _bb;
    }

    public Vec2i __assign(int _i, ByteBuffer _bb) {
        __init(_i, _bb);
        return this;
    }

    public int x() {
        return bb.getInt(bb_pos + 0);
    }

    public int y() {
        return bb.getInt(bb_pos + 4);
    }
}

