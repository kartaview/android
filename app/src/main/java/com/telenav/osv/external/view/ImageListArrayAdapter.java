//package com.telenav.osv.external.view;
//
//import java.util.List;
//import android.content.Context;
//import android.graphics.BitmapFactory;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.ArrayAdapter;
//import android.widget.ImageView;
//import android.widget.TextView;
//import com.telenav.osv.R;
//
///**
// * Adapter class for photo list display
// */
//public class ImageListArrayAdapter extends ArrayAdapter<ImageItem> {
//
//	private List<ImageItem> rows;
//	private LayoutInflater inflater;
//
//	/**
//	 * Constructor
//	 * @param context Context
//	 * @param resourceIdOfListLayout Resource ID for specifying line information
//	 * @param rows Line object
//     */
//	public ImageListArrayAdapter(Context context, int resourceIdOfListLayout, List<ImageItem> rows) {
//		super(context, resourceIdOfListLayout, rows);
//		this.rows = rows;
//		this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//	}
//
//    /**
//     * getView Method
//     * @param position Acquisition position
//     * @param convertView convertView object
//     * @param parent Parent object for list
//     * @return View instance
//     */
//	@Override
//	public View getView(int position, View convertView, ViewGroup parent) {
//		View view = convertView;
//		if (view == null) {
//			view = inflater.inflate(R.layout.listlayout_object, null);
//		}
//
//		ImageItem row = rows.get(position);
//
//		ImageView thumbnail = (ImageView) view.findViewById(R.id.object_thumbnail);
//		if (row.isPhoto()) {
//			byte[] thumbnailImage = row.getThumbnail();
//			thumbnail.setImageBitmap(BitmapFactory.decodeByteArray(thumbnailImage, 0, thumbnailImage.length));
//		} else {
//			thumbnail.setImageBitmap(null);
//		}
//
//		TextView title = (TextView) view.findViewById(R.id.list_item_title);
//		title.setText(row.getFileName());
//
//		TextView description = (TextView) view.findViewById(R.id.list_item_description);
//		description.setText(row.getCaptureDate());
//
//		return view;
//	}
//}
