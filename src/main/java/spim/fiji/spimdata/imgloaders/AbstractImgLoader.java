package spim.fiji.spimdata.imgloaders;

import java.util.HashMap;
import java.util.List;

import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import mpicbg.spim.data.sequence.ImgLoader;
import mpicbg.spim.data.sequence.ViewId;
import mpicbg.spim.data.sequence.ViewSetup;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.img.ImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;

public abstract class AbstractImgLoader implements ImgLoader< UnsignedShortType >
{
	protected ImgFactory< ? extends NativeType< ? > > imgFactory;
	
	private final HashMap< ViewId, Pair< Dimensions, VoxelDimensions > > imageMetaDataCache;
	private final HashMap< Integer, ViewId > viewIdLookUp;

	protected AbstractImgLoader()
	{
		imgFactory = null;
		imageMetaDataCache = new HashMap< ViewId, Pair< Dimensions, VoxelDimensions > >();
		viewIdLookUp = new HashMap< Integer, ViewId >();
	}

	/**
	 * Updates the cached imageMetaData
	 */
	protected void updateMetaDataCache( final ViewId viewId,
			final int w, final int h, final int d,
			final double calX, final double calY, final double calZ )
	{
		imageMetaDataCache.put( viewId, new ValuePair< Dimensions, VoxelDimensions >(
				new FinalDimensions( new long[] { w, h, d } ),
				new FinalVoxelDimensions( "", calX, calY, calZ ) ) );

		// links the viewSetupId to the last added viewId, overwrites earlier entries
		viewIdLookUp.put( viewId.getViewSetupId(), viewId );
	}

	/**
	 * Loads only the metadata from the image, should call updateMetaDataCache( ... )
	 * @param view
	 */
	protected abstract void loadMetaData( final ViewId view );

	@Override
	public Dimensions getImageSize( final ViewId view )
	{
		// if there is no data for the viewId
		if ( !imageMetaDataCache.containsKey( view ) )
		{
			// check if the data is present for the same viewsetup of another timepoint
			if ( !viewIdLookUp.containsKey( view.getViewSetupId() ) )
				loadMetaData( view );
			else
				return imageMetaDataCache.get( viewIdLookUp.get( view.getViewSetupId() ) ).getA();
		}
		return imageMetaDataCache.get( view ).getA();
	}

	@Override
	public VoxelDimensions getVoxelSize( final ViewId view )
	{
		// if there is no data for the viewId
		if ( !imageMetaDataCache.containsKey( view ) )
		{
			// check if the data is present for the same viewsetup of another timepoint
			if ( !viewIdLookUp.containsKey( view.getViewSetupId() ) )
				loadMetaData( view );
			else
				return imageMetaDataCache.get( viewIdLookUp.get( view.getViewSetupId() ) ).getB();
		}
		return imageMetaDataCache.get( view ).getB();
	}
	
	@Override
	public UnsignedShortType getImageType()
	{
		return new UnsignedShortType();
	}	
		
	public ImgFactory< ? extends NativeType< ? > > getImgFactory() { return imgFactory; }
	public void setImgFactory( final ImgFactory< ? extends NativeType< ? > > imgFactory ) { this.imgFactory = imgFactory; }

	/**
	 * Updates the ViewSetups using the imageMetaDataCache
	 * 
	 * @param data - the {@link SpimData} object that contains all {@link ViewSetup}s can could be potentially updated
	 * @param forceUpdate - overwrite the data if it is already present
	 */
	public void updateXMLMetaData( final SpimData data, final boolean forceUpdate )
	{	
		updateXMLMetaData( data.getSequenceDescription().getViewSetupsOrdered(), forceUpdate );
	}

	/**
	 * Updates a list of ViewSetups using the imageMetaDataCache
	 * 
	 * @param setups - a list of {@link ViewSetup}s can could be potentially updated
	 * @param forceUpdate - overwrite the data if it is already present
	 */
	public void updateXMLMetaData( final List< ? extends ViewSetup > setups, final boolean forceUpdate )
	{
		for ( final ViewSetup setup : setups )
			updateXMLMetaData( setup, forceUpdate );
	}

	/**
	 * Updates one specific ViewSetup using the imageMetaDataCache
	 * 
	 * @param setup - {@link ViewSetup}s that can potentially be updated if it is in the cache
	 * @param forceUpdate - overwrite the data if it is already present
	 * @return true if something was updated, false if it was not in the cache or if could have been updated but was already there
	 */
	public boolean updateXMLMetaData( final ViewSetup setup, final boolean forceUpdate )
	{
		boolean updated = false;
		
		if ( viewIdLookUp.containsKey( setup.getId() ) )
		{
			// look up the metadata using the ViewId linked by the ViewSetupId
			final Pair< Dimensions, VoxelDimensions > metaData = imageMetaDataCache.get( viewIdLookUp.get( setup.getId() ) );

			if ( !setup.hasSize() || forceUpdate )
			{
				setup.setSize( metaData.getA() );
				updated = true;
			}

			if ( !setup.hasVoxelSize() || forceUpdate )
			{
				setup.setVoxelSize( metaData.getB() );
				updated = true;
			}
		}
		
		return updated;
	}
}
