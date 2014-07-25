package rajawali.postprocessing.passes;

import rajawali.framework.R;


public class CopyPass extends EffectPass {
	public CopyPass()
	{
<<<<<<< HEAD
		createMaterial(R.raw.minimal_vertex_shader, R.raw.copy_fragment_shader);	
=======
		super();
		mCustomVertexShader = new MinimalVertexShader();
		mCustomFragmentShader = new CopyFragmentShader();
	}
	
	public void setOpacity(float opacity)
	{
		((CopyFragmentShader)mCustomFragmentShader).setOpacity(opacity);
	}
	
	private class MinimalVertexShader extends VertexShader
	{
		public MinimalVertexShader() {
			super();
			mNeedsBuild = false;
			mShaderString = RawShaderLoader.fetch(R.raw.minimal_vertex_shader);
		}
	}
	
	private class CopyFragmentShader extends FragmentShader
	{
		private int muOpacityHandle;
		private float mOpacity;
		
		public CopyFragmentShader() {
			super();
			mNeedsBuild = false;
			mShaderString = RawShaderLoader.fetch(R.raw.copy_fragment_shader);
		}
		
		@Override
		public void setLocations(final int programHandle)
		{
			super.setLocations(programHandle);
			muOpacityHandle = getUniformLocation(programHandle, "uOpacity");
		}
		
		@Override
		public void applyParams()
		{
			super.applyParams();
			GLES20.glUniform1f(muOpacityHandle, mOpacity);
		}
		
		public void setOpacity(float opacity)
		{
			mOpacity = opacity;
		}
>>>>>>> upstream/master
	}
}
