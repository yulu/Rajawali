package rajawali.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Stack;
import java.util.StringTokenizer;

import rajawali.BaseObject3D;
import rajawali.materials.AMaterial;
import rajawali.materials.DiffuseMaterial;
import rajawali.materials.NormalMapMaterial;
import rajawali.materials.NormalMapPhongMaterial;
import rajawali.materials.PhongMaterial;
import rajawali.materials.textures.ATexture.TextureException;
import rajawali.materials.textures.NormalMapTexture;
import rajawali.materials.textures.SpecularMapTexture;
import rajawali.materials.textures.Texture;
import rajawali.materials.textures.TextureManager;
import rajawali.renderer.RajawaliRenderer;
import rajawali.util.RajLog;
import rajawali.wallpaper.Wallpaper;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Log;

/**
 * The most important thing is that the model should be triangulated. Rajawali doesn�t accept quads, only tris. In
 * Blender, this is an option you can select in the exporter. In a program like MeshLab, this is done automatically. At
 * the moment, Rajawali also doesn�t support per-face textures. This is on the todo list.
 * <p>
 * The options that should be checked when exporting from blender are:
 * <ul>
 * <li>Apply Modifiers
 * <li>Include Normals
 * <li>Include UVs
 * <li>Write Materials (if applicable)
 * <li>Triangulate Faces
 * <li>Objects as OBJ Objects
 * </ul>
 * <p>
 * The files should be written to your �res/raw� folder in your ADT project. Usually you�ll get errors in the console
 * when you do this. The Android SDK ignores file extensions so it�ll regard the .obj and .mtl files as duplicates. The
 * way to fix this is to rename the files. For instance: - myobject.obj > myobject_obj - myobject.mtl > myobject_mtl The
 * parser replaces any dots in file names, so this should be picked up automatically by the parser. Path fragments in
 * front of file names (also texture paths) are discarded so you can leave them as is.
 * <p>
 * The texture file paths in the .mtl files are stripped off periods and path fragments as well. The textures need to be
 * placed in the res/drawable-nodpi folder.
 * <p>
 * If it still throws errors check if there are any funny characters or unsupported texture formats (like bmp).
 * <p>
 * Just as a reminder, here�s the code that takes care of the parsing:
 * 
 * <pre>
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * {
 * 	&#064;code
 * 	ObjParser objParser = new ObjParser(mContext.getResources(), mTextureManager, R.raw.myobject_obj);
 * 	objParser.parse();
 * 	BaseObject3D mObject = objParser.getParsedObject();
 * 	mObject.setLight(mLight);
 * 	addChild(mObject);
 * }
 * </pre>
 * 
 * @author dennis.ippel
 * 
 */
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
						if (lineArr[i].length() >= 5)
							face.normalIndices.add(parseIndex(
									Integer.parseInt(lineArr[i].substring(prev, lineArr[i].length())),
									mNormals.size()));

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

			final ArrayList<Integer> indis = mCurrentMaterialGroup.faces.get(0).vertexIndices;
			final int[] indi = new int[indis.size()];
			for (int i = 0; i < indi.length; i++)
				indi[i] = indis.get(i);

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
