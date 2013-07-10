package rajawali.parser;

import java.io.BufferedReader;
import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

import rajawali.materials.textures.Texture;
import rajawali.materials.textures.TextureManager;
import rajawali.renderer.RajawaliRenderer;
import rajawali.util.RajLog;
import android.content.res.Resources;

public class ObjParser extends AMeshParser {

	private enum LineType {
		MTLLIB, G, O, USEMTL, V, VT, VN, F
	}

	private final ArrayList<Float> mVertices = new ArrayList<Float>();
	private final ArrayList<Float> mNormals = new ArrayList<Float>();
	private final ArrayList<String> mMaterialIds = new ArrayList<String>();
	private final ArrayList<Float> mUvs = new ArrayList<Float>();
	private final ArrayList<ObjectGroup> mObjects = new ArrayList<ObjectGroup>();

	private boolean mTlib;
	private boolean mTlibLoaded;
	private Group mCurrentGroup;
	private ObjectGroup mCurrentObject;
	private MaterialGroup mCurrentMaterialGroup;
	private String mActiveMaterialId;

	public ObjParser(RajawaliRenderer renderer, String fileOnSDCard) {
		super(renderer, fileOnSDCard);
	}

	public ObjParser(RajawaliRenderer renderer, int resourceId) {
		this(renderer.getContext().getResources(), renderer.getTextureManager(), resourceId);
	}

	public ObjParser(Resources resources, TextureManager textureManager, int resourceId) {
		super(resources, textureManager, resourceId);
	}

	public ObjParser(RajawaliRenderer renderer, File file) {
		super(renderer, file);
	}

	@Override
	public ObjParser parse() throws ParsingException {
		super.parse();

		final BufferedReader reader;
		try {
			reader = getBufferedReader();
		} catch (Exception e) {
			throw new ParsingException(e);
		}

		// Begin reading OBJ
		try {
			String line;
			while ((line = reader.readLine()) != null) {

				// Ignore comments
				if (line.startsWith("#")) {
					RajLog.i(this + " OBJ COMMENT: " + line);
					continue;
				}

				// Merge line breaks (line ending in /)
				if (line.endsWith("/"))
					line = line.substring(0, line.length() - 1) + reader.readLine();

				// Fix garbage lines
				line = line.trim();
				while (line.indexOf("  ") != -1)
					line = line.replace("  ", " ");

				// Skip empty lines
				if (line.length() == 0)
					continue;

				// Split on the spaces
				final String[] lineArr = line.split(" ");

				// Determine the type of the line
				final LineType lineType = LineType.valueOf(lineArr[0].toUpperCase(
						Locale.ENGLISH));

				// Process the type
				switch (lineType) {
				case F: // Face
					final FaceData face = new FaceData();

					if (mCurrentMaterialGroup == null)
						createGroup(null);

					for (int i = 1; i < 4; i++) {
						int nextSlash = lineArr[i].indexOf("/");
						final int vertexIndice = parseIndex(Integer.parseInt(lineArr[i].substring(0, nextSlash)),
								mVertices.size());

						// Vertex Indices
						face.vertexIndices.add(vertexIndice);

						int prev = nextSlash + 1;
						nextSlash = lineArr[i].indexOf("/", prev);

						// UV Indices
						if (lineArr[i].length() >= 3)
							face.uvIndices.add(parseIndex(Integer.parseInt(lineArr[i].substring(prev, nextSlash)),
									mUvs.size()));

						prev = nextSlash + 1;

						// Normal Indices
						if (lineArr[i].length() >= 5) {
							face.normalIndices.add(parseIndex(
									Integer.parseInt(lineArr[i].substring(prev, lineArr[i].length())),
									mNormals.size()));
						}

						face.indexIds.add(vertexIndice);
					}

					mCurrentMaterialGroup.faces.add(face);

					break;
				case G: // Object Groups
					createGroup(line.substring(2, line.length()));
					break;
				case MTLLIB:
					mTlib = true;
					mTlibLoaded = false;
					break;
				case O: // Objects
					createObject(line.substring(2, line.length()));
					break;
				case USEMTL:
					break;
				case V: // Vertices
					mVertices.add(Float.parseFloat(lineArr[1]));
					mVertices.add(Float.parseFloat(lineArr[2]));
					mVertices.add(Float.parseFloat(lineArr[3]));
					break;
				case VN: // Normals
					mNormals.add(Float.parseFloat(lineArr[1]));
					mNormals.add(Float.parseFloat(lineArr[2]));
					mNormals.add(Float.parseFloat(lineArr[3]));
					break;
				case VT: // UV
					mUvs.add(Float.parseFloat(lineArr[1]));
					mUvs.add(Float.parseFloat(lineArr[2]));
					break;
				}
			}

			final float[] verts = new float[mVertices.size()];
			for (int i = 0; i < verts.length; i++)
				verts[i] = mVertices.get(i);

			final float[] norms = new float[mNormals.size()];
			for (int i = 0; i < norms.length; i++)
				norms[i] = mNormals.get(i);

			final float[] uvs = new float[mUvs.size()];
			for (int i = 0; i < uvs.length; i++)
				uvs[i] = mUvs.get(i);

			final int[] indi = new int[mCurrentMaterialGroup.faces.size() * 3];
			for (int k = 0, j = mCurrentMaterialGroup.faces.size(); k < j; k++) {
				final ArrayList<Integer> indis = mCurrentMaterialGroup.faces.get(k).vertexIndices;
				for (int i = 0, l = indis.size(); i < l; i++)
					indi[(k * 3) + i] = indis.get(i);
			}

			mRootObject.setData(verts, norms, uvs, null, indi);
		} catch (Exception e) {
			e.printStackTrace();
			throw new ParsingException(e);
		}

		return this;
	}

	private void createGroup(String line) {
		if (mCurrentObject == null)
			createObject(null);

		mCurrentGroup = new Group();
		mCurrentGroup.materialId = mActiveMaterialId;
		mCurrentGroup.name = line;

		mCurrentObject.groups.add(mCurrentGroup);
		createMaterialGroup(null);
	}

	private void createMaterialGroup(String line) {
		mCurrentMaterialGroup = new MaterialGroup();
		mCurrentMaterialGroup.path = line;
		mCurrentGroup.materialGroups.add(mCurrentMaterialGroup);
	}

	private void createObject(String line) {
		// Create a new object
		mCurrentObject = new ObjectGroup();
		mCurrentObject.name = line;

		mCurrentGroup = null;
		mCurrentMaterialGroup = null;
		mObjects.add(mCurrentObject);
	}

	private int parseIndex(int index, int length) {
		return index < 0 ? index + length + 1 : index;
	}

	private final static class ObjectGroup {

		public final ArrayList<Group> groups = new ArrayList<ObjParser.Group>();

		public String name;
	}

	private final static class Group {

		public final ArrayList<MaterialGroup> materialGroups = new ArrayList<MaterialGroup>();

		public String name;
		public String materialId;
	}

	private final static class MaterialGroup {

		public final ArrayList<FaceData> faces = new ArrayList<FaceData>();

		public String path;
	}

	private final static class SpecularData {

		public String materialId;
		public int ambientColor = 0xFFFFFF;
		public float alpha = 1;
	}

	private final static class LoadedMaterials {

		public String materialId;
		public Texture texture;
		public int ambientColor = 0xFFFFFF;
		public float alpha = 1;
	}

	private final static class FaceData {

		public final ArrayList<Integer> vertexIndices = new ArrayList<Integer>();
		public final ArrayList<Integer> uvIndices = new ArrayList<Integer>();
		public final ArrayList<Integer> normalIndices = new ArrayList<Integer>();
		public final ArrayList<Integer> indexIds = new ArrayList<Integer>();
	}
}
