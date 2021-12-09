// automatically generated by the FlatBuffers compiler, do not modify

package com.google.ar.sceneform.lullmodel;

import com.google.flatbuffers.FlatBufferBuilder;
import com.google.flatbuffers.Struct;

import java.nio.ByteBuffer;

@SuppressWarnings("unused")
/**
 * Describes a single attribute in vertex format.
 */
public final class VertexAttribute extends Struct {
    public static int createVertexAttribute(FlatBufferBuilder builder, int usage, int type) {
        builder.prep(4, 8);
        builder.putInt(type);
        builder.putInt(usage);
        return builder.offset();
    }

    public void __init(int _i, ByteBuffer _bb) {
        bb_pos = _i;
        bb = _bb;
    }

    public VertexAttribute __assign(int _i, ByteBuffer _bb) {
        __init(_i, _bb);
        return this;
    }

    public int usage() {
        return bb.getInt(bb_pos + 0);
    }

    public int type() {
        return bb.getInt(bb_pos + 4);
    }
}

