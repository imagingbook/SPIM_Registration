/*-
 * #%L
 * Fiji distribution of ImageJ for the life sciences.
 * %%
 * Copyright (C) 2007 - 2017 Fiji development team
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */
package spim.fiji.spimdata.imgloaders;

import static mpicbg.spim.data.XmlHelpers.loadPath;
import static mpicbg.spim.data.XmlKeys.IMGLOADER_FORMAT_ATTRIBUTE_NAME;

import java.io.File;

import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.img.planar.PlanarImgFactory;
import net.imglib2.type.numeric.real.FloatType;

import org.jdom2.Element;

import mpicbg.spim.data.XmlHelpers;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.ImgLoaderIo;
import mpicbg.spim.data.generic.sequence.XmlIoBasicImgLoader;

@ImgLoaderIo( format = "spimreconstruction.lightsheetz1", type = LightSheetZ1ImgLoader.class )
public class XmlIoLightSheetZ1ImgLoader implements XmlIoBasicImgLoader< LightSheetZ1ImgLoader >
{
	public static final String DIRECTORY_TAG = "imagedirectory";
	public static final String MASTER_FILE_TAG = "masterfile";
	public static final String IMGLIB2CONTAINER_PATTERN_TAG = "imglib2container";

	@Override
	public Element toXml( final LightSheetZ1ImgLoader imgLoader, final File basePath )
	{
		final Element elem = new Element( "ImageLoader" );
		elem.setAttribute( IMGLOADER_FORMAT_ATTRIBUTE_NAME, this.getClass().getAnnotation( ImgLoaderIo.class ).format() );

		elem.addContent( XmlHelpers.pathElement( DIRECTORY_TAG, imgLoader.getCZIFile().getParentFile(), basePath ) );
		elem.addContent( XmlHelpers.textElement( MASTER_FILE_TAG, imgLoader.getCZIFile().getName() ) );
		elem.addContent( XmlHelpers.textElement( IMGLIB2CONTAINER_PATTERN_TAG, imgLoader.getImgFactory().getClass().getSimpleName() ) );
		
		return elem;
	}

	@Override
	public LightSheetZ1ImgLoader fromXml(
			final Element elem, File basePath,
			final AbstractSequenceDescription<?, ?, ?> sequenceDescription )
	{
		try
		{
			final File path = loadPath( elem, DIRECTORY_TAG, basePath );
			final String masterFile = XmlHelpers.getText( elem, MASTER_FILE_TAG );
			final String container = XmlHelpers.getText( elem, IMGLIB2CONTAINER_PATTERN_TAG );

			final ImgFactory< FloatType > imgFactory;

			if ( container == null )
			{
				System.out.println( "WARNING: No Img implementation defined in XML, using ArrayImg." );

				// if no factory is defined we define an ArrayImgFactory
				imgFactory = new ArrayImgFactory< FloatType >();
			}
			else
			{
				if ( container.toLowerCase().contains( "cellimg" ) )
				{
					imgFactory = new CellImgFactory< FloatType >( 256 );
				}
				else if ( container.toLowerCase().contains( "arrayimg" ) )
				{
					imgFactory = new ArrayImgFactory< FloatType >();
				}
				else if ( container.toLowerCase().contains( "planarimg" ) )
				{
					imgFactory = new PlanarImgFactory< FloatType >();
				}
				else
				{
					// if factory is unknown we define an ArrayImgFactory
					imgFactory = new ArrayImgFactory< FloatType >();
					
					System.out.println( "WARNING: Unknown Img implementation defined in XML:'" + container + "', using ArrayImg." );
				}
			}

			return new LightSheetZ1ImgLoader( new File( path, masterFile ), imgFactory, sequenceDescription );
		}
		catch ( final Exception e )
		{
			throw new RuntimeException( e );
		}
	}

}
