// automatically generated by the FlatBuffers compiler, do not modify

package com.google.ar.sceneform.lullmodel;

import com.google.flatbuffers.FlatBufferBuilder;
import com.google.flatbuffers.Table;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@SuppressWarnings("unused")
/**
 * A single instance of model data used to render an object at a given LOD.
 */
public final class ModelInstanceDef extends Table {
    public static ModelInstanceDef getRootAsModelInstanceDef(ByteBuffer _bb) {
        return getRootAsModelInstanceDef(_bb, new ModelInstanceDef());
    }

    public static ModelInstanceDef getRootAsModelInstanceDef(ByteBuffer _bb, ModelInstanceDef obj) {
        _bb.order(ByteOrder.LITTLE_ENDIAN);
        return (obj.__assign(_bb.getInt(_bb.position()) + _bb.position(), _bb));
    }

    public static int createModelInstanceDef(FlatBufferBuilder builder,
                                             int vertex_dataOffset,
                                             int indices16Offset,
                                             int indices32Offset,
                                             int rangesOffset,
                                             int materialsOffset,
                                             int vertex_attributesOffset,
                                             long num_vertices,
                                             boolean interleaved,
                                             int shader_to_mesh_bonesOffset,
                                             int blend_shapesOffset,
                                             int blend_attributesOffset,
                                             int aabbsOffset) {
        builder.startObject(12);
        ModelInstanceDef.addAabbs(builder, aabbsOffset);
        ModelInstanceDef.addBlendAttributes(builder, blend_attributesOffset);
        ModelInstanceDef.addBlendShapes(builder, blend_shapesOffset);
        ModelInstanceDef.addShaderToMeshBones(builder, shader_to_mesh_bonesOffset);
        ModelInstanceDef.addNumVertices(builder, num_vertices);
        ModelInstanceDef.addVertexAttributes(builder, vertex_attributesOffset);
        ModelInstanceDef.addMaterials(builder, materialsOffset);
        ModelInstanceDef.addRanges(builder, rangesOffset);
        ModelInstanceDef.addIndices32(builder, indices32Offset);
        ModelInstanceDef.addIndices16(builder, indices16Offset);
        ModelInstanceDef.addVertexData(builder, vertex_dataOffset);
        ModelInstanceDef.addInterleaved(builder, interleaved);
        return ModelInstanceDef.endModelInstanceDef(builder);
    }

    public static void startModelInstanceDef(FlatBufferBuilder builder) {
        builder.startObject(12);
    }

    public static void addVertexData(FlatBufferBuilder builder, int vertexDataOffset) {
        builder.addOffset(0, vertexDataOffset, 0);
    }

    public static int createVertexDataVector(FlatBufferBuilder builder, byte[] data) {
        return builder.createByteVector(data);
    }

    public static int createVertexDataVector(FlatBufferBuilder builder, ByteBuffer data) {
        return builder.createByteVector(data);
    }

    public static void startVertexDataVector(FlatBufferBuilder builder, int numElems) {
        builder.startVector(1, numElems, 1);
    }

    public static void addIndices16(FlatBufferBuilder builder, int indices16Offset) {
        builder.addOffset(1, indices16Offset, 0);
    }

    public static int createIndices16Vector(FlatBufferBuilder builder, short[] data) {
        builder.startVector(2, data.length, 2);
        for (int i = data.length - 1; i >= 0; i--) builder.addShort(data[i]);
        return builder.endVector();
    }

    public static void startIndices16Vector(FlatBufferBuilder builder, int numElems) {
        builder.startVector(2, numElems, 2);
    }

    public static void addIndices32(FlatBufferBuilder builder, int indices32Offset) {
        builder.addOffset(2, indices32Offset, 0);
    }

    public static int createIndices32Vector(FlatBufferBuilder builder, int[] data) {
        builder.startVector(4, data.length, 4);
        for (int i = data.length - 1; i >= 0; i--) builder.addInt(data[i]);
        return builder.endVector();
    }

    public static void startIndices32Vector(FlatBufferBuilder builder, int numElems) {
        builder.startVector(4, numElems, 4);
    }

    public static void addRanges(FlatBufferBuilder builder, int rangesOffset) {
        builder.addOffset(3, rangesOffset, 0);
    }

    public static void startRangesVector(FlatBufferBuilder builder, int numElems) {
        builder.startVector(8, numElems, 4);
    }

    public static void addMaterials(FlatBufferBuilder builder, int materialsOffset) {
        builder.addOffset(4, materialsOffset, 0);
    }

    public static int createMaterialsVector(FlatBufferBuilder builder, int[] data) {
        builder.startVector(4, data.length, 4);
        for (int i = data.length - 1; i >= 0; i--) builder.addOffset(data[i]);
        return builder.endVector();
    }

    public static void startMaterialsVector(FlatBufferBuilder builder, int numElems) {
        builder.startVector(4, numElems, 4);
    }

    public static void addVertexAttributes(FlatBufferBuilder builder, int vertexAttributesOffset) {
        builder.addOffset(5, vertexAttributesOffset, 0);
    }

    public static void startVertexAttributesVector(FlatBufferBuilder builder, int numElems) {
        builder.startVector(8, numElems, 4);
    }

    public static void addNumVertices(FlatBufferBuilder builder, long numVertices) {
        builder.addInt(6, (int) numVertices, (int) 0L);
    }

    public static void addInterleaved(FlatBufferBuilder builder, boolean interleaved) {
        builder.addBoolean(7, interleaved, true);
    }

    public static void addShaderToMeshBones(FlatBufferBuilder builder, int shaderToMeshBonesOffset) {
        builder.addOffset(8, shaderToMeshBonesOffset, 0);
    }

    public static int createShaderToMeshBonesVector(FlatBufferBuilder builder, byte[] data) {
        return builder.createByteVector(data);
    }

    public static int createShaderToMeshBonesVector(FlatBufferBuilder builder, ByteBuffer data) {
        return builder.createByteVector(data);
    }

    public static void startShaderToMeshBonesVector(FlatBufferBuilder builder, int numElems) {
        builder.startVector(1, numElems, 1);
    }

    public static void addBlendShapes(FlatBufferBuilder builder, int blendShapesOffset) {
        builder.addOffset(9, blendShapesOffset, 0);
    }

    public static int createBlendShapesVector(FlatBufferBuilder builder, int[] data) {
        builder.startVector(4, data.length, 4);
        for (int i = data.length - 1; i >= 0; i--) builder.addOffset(data[i]);
        return builder.endVector();
    }

    public static void startBlendShapesVector(FlatBufferBuilder builder, int numElems) {
        builder.startVector(4, numElems, 4);
    }

    public static void addBlendAttributes(FlatBufferBuilder builder, int blendAttributesOffset) {
        builder.addOffset(10, blendAttributesOffset, 0);
    }

    public static void startBlendAttributesVector(FlatBufferBuilder builder, int numElems) {
        builder.startVector(8, numElems, 4);
    }

    public static void addAabbs(FlatBufferBuilder builder, int aabbsOffset) {
        builder.addOffset(11, aabbsOffset, 0);
    }

    public static int createAabbsVector(FlatBufferBuilder builder, int[] data) {
        builder.startVector(4, data.length, 4);
        for (int i = data.length - 1; i >= 0; i--) builder.addOffset(data[i]);
        return builder.endVector();
    }

    public static void startAabbsVector(FlatBufferBuilder builder, int numElems) {
        builder.startVector(4, numElems, 4);
    }

    public static int endModelInstanceDef(FlatBufferBuilder builder) {
        int o = builder.endObject();
        return o;
    }

    public void __init(int _i, ByteBuffer _bb) {
        bb_pos = _i;
        bb = _bb;
        vtable_start = bb_pos - bb.getInt(bb_pos);
        vtable_size = bb.getShort(vtable_start);
    }

    public ModelInstanceDef __assign(int _i, ByteBuffer _bb) {
        __init(_i, _bb);
        return this;
    }

    /**
     * The "raw" vertex data stored as a byte array.
     */
    public int vertexData(int j) {
        int o = __offset(4);
        return o != 0 ? bb.get(__vector(o) + j * 1) & 0xFF : 0;
    }

    public int vertexDataLength() {
        int o = __offset(4);
        return o != 0 ? __vector_len(o) : 0;
    }

    public ByteBuffer vertexDataAsByteBuffer() {
        return __vector_as_bytebuffer(4, 1);
    }

    public ByteBuffer vertexDataInByteBuffer(ByteBuffer _bb) {
        return __vector_in_bytebuffer(_bb, 4, 1);
    }

    /**
     * Indices into the vertex data. Will either be an array of 16-bit or 32-bit
     * values.
     */
    public int indices16(int j) {
        int o = __offset(6);
        return o != 0 ? bb.getShort(__vector(o) + j * 2) & 0xFFFF : 0;
    }

    public int indices16Length() {
        int o = __offset(6);
        return o != 0 ? __vector_len(o) : 0;
    }

    public ByteBuffer indices16AsByteBuffer() {
        return __vector_as_bytebuffer(6, 2);
    }

    public ByteBuffer indices16InByteBuffer(ByteBuffer _bb) {
        return __vector_in_bytebuffer(_bb, 6, 2);
    }

    public long indices32(int j) {
        int o = __offset(8);
        return o != 0 ? (long) bb.getInt(__vector(o) + j * 4) & 0xFFFFFFFFL : 0;
    }

    public int indices32Length() {
        int o = __offset(8);
        return o != 0 ? __vector_len(o) : 0;
    }

    public ByteBuffer indices32AsByteBuffer() {
        return __vector_as_bytebuffer(8, 4);
    }

    public ByteBuffer indices32InByteBuffer(ByteBuffer _bb) {
        return __vector_in_bytebuffer(_bb, 8, 4);
    }

    /**
     * The range of indices for each submesh.
     */
    public ModelIndexRange ranges(int j) {
        return ranges(new ModelIndexRange(), j);
    }

    public ModelIndexRange ranges(ModelIndexRange obj, int j) {
        int o = __offset(10);
        return o != 0 ? obj.__assign(__vector(o) + j * 8, bb) : null;
    }

    public int rangesLength() {
        int o = __offset(10);
        return o != 0 ? __vector_len(o) : 0;
    }

    /**
     * The material describing the "look" of each submesh.
     */
    public MaterialDef materials(int j) {
        return materials(new MaterialDef(), j);
    }

    public MaterialDef materials(MaterialDef obj, int j) {
        int o = __offset(12);
        return o != 0 ? obj.__assign(__indirect(__vector(o) + j * 4), bb) : null;
    }

    public int materialsLength() {
        int o = __offset(12);
        return o != 0 ? __vector_len(o) : 0;
    }

    /**
     * Describes the structure of the vertex data, effectively the VertexFormat.
     */
    public VertexAttribute vertexAttributes(int j) {
        return vertexAttributes(new VertexAttribute(), j);
    }

    public VertexAttribute vertexAttributes(VertexAttribute obj, int j) {
        int o = __offset(14);
        return o != 0 ? obj.__assign(__vector(o) + j * 8, bb) : null;
    }

    public int vertexAttributesLength() {
        int o = __offset(14);
        return o != 0 ? __vector_len(o) : 0;
    }

    /**
     * The total number of vertices stored in the vertex data.
     */
    public long numVertices() {
        int o = __offset(16);
        return o != 0 ? (long) bb.getInt(o + bb_pos) & 0xFFFFFFFFL : 0L;
    }

    /**
     * Whether or not the attributes in the vertex data are interleaved.
     */
    public boolean interleaved() {
        int o = __offset(18);
        return o == 0 || 0 != bb.get(o + bb_pos);
    }

    /**
     * Maps the skeleton bone index to the shader bone index. The shader bones
     * are only the bones that have at least one vertex weighted to them and, as
     * such, are a subset of all the bones in the skeleton.
     */
    public int shaderToMeshBones(int j) {
        int o = __offset(20);
        return o != 0 ? bb.get(__vector(o) + j * 1) & 0xFF : 0;
    }

    public int shaderToMeshBonesLength() {
        int o = __offset(20);
        return o != 0 ? __vector_len(o) : 0;
    }

    public ByteBuffer shaderToMeshBonesAsByteBuffer() {
        return __vector_as_bytebuffer(20, 1);
    }

    public ByteBuffer shaderToMeshBonesInByteBuffer(ByteBuffer _bb) {
        return __vector_in_bytebuffer(_bb, 20, 1);
    }

    /**
     * A collection of blendshapes, if they exist.
     */
    public BlendShape blendShapes(int j) {
        return blendShapes(new BlendShape(), j);
    }

    public BlendShape blendShapes(BlendShape obj, int j) {
        int o = __offset(22);
        return o != 0 ? obj.__assign(__indirect(__vector(o) + j * 4), bb) : null;
    }

    public int blendShapesLength() {
        int o = __offset(22);
        return o != 0 ? __vector_len(o) : 0;
    }

    /**
     * Describes the structure of the vertex data for the blend shapes.
     */
    public VertexAttribute blendAttributes(int j) {
        return blendAttributes(new VertexAttribute(), j);
    }

    public VertexAttribute blendAttributes(VertexAttribute obj, int j) {
        int o = __offset(24);
        return o != 0 ? obj.__assign(__vector(o) + j * 8, bb) : null;
    }

    public int blendAttributesLength() {
        int o = __offset(24);
        return o != 0 ? __vector_len(o) : 0;
    }

    /**
     * A bounding Aabb for each submesh.
     */
    public SubmeshAabb aabbs(int j) {
        return aabbs(new SubmeshAabb(), j);
    }

    public SubmeshAabb aabbs(SubmeshAabb obj, int j) {
        int o = __offset(26);
        return o != 0 ? obj.__assign(__indirect(__vector(o) + j * 4), bb) : null;
    }

    public int aabbsLength() {
        int o = __offset(26);
        return o != 0 ? __vector_len(o) : 0;
    }
}

