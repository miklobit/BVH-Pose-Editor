package com.akjava.gwt.poseeditor.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.akjava.gwt.html5.client.file.File;
import com.akjava.gwt.html5.client.file.FileHandler;
import com.akjava.gwt.html5.client.file.FileReader;
import com.akjava.gwt.html5.client.file.FileUtils;
import com.akjava.gwt.lib.client.LogUtils;
import com.akjava.gwt.lib.client.StorageControler;
import com.akjava.gwt.lib.client.StorageDataList;
import com.akjava.gwt.lib.client.StorageDataList.HeaderAndValue;
import com.akjava.gwt.lib.client.ValueUtils;
import com.akjava.gwt.poseeditor.client.resources.PoseEditorBundles;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class PreferenceTabPanel extends VerticalPanel {
	private StorageControler controler;
	private PreferenceListener listener;
	private Map<Integer, String> presetModelMap = new HashMap<Integer, String>();
	private ListBox modelListBox;
	private Label modelSelection;

	private StorageDataList modelControler;
	public static final String KEY_MODEL_SELECTION="MODEL_SELECTION";
	private Button removeBt;
	public PreferenceListener getListener() {
		return listener;
	}

	public void setListener(PreferenceListener listener) {
		this.listener = listener;
	}

	public PreferenceTabPanel(StorageControler cs,
			PreferenceListener listener) {
		this.controler = cs;
		this.listener = listener;

		FileUpload meshUpload=new FileUpload();
		add(meshUpload);
		meshUpload.addChangeHandler(new ChangeHandler() {
			
			@Override
			public void onChange(ChangeEvent event) {
				JsArray<File> files = FileUtils.toFile(event.getNativeEvent());
				
				final FileReader reader=FileReader.createFileReader();
				final File file=files.get(0);
				reader.setOnLoad(new FileHandler() {
					@Override
					public void onLoad() {
						String text=reader.getResultAsString();
						JSONValue lastJsonValue = JSONParser.parseLenient(text);
						//TODO more validate
						JSONObject object=lastJsonValue.isObject();
						if(object==null){
							Window.alert("invalid model");
							return;
						}
						
						modelControler.setDataValue(file.getFileName(), text);
						int id=modelControler.incrementId();
						
						loadModel(new HeaderAndValue(id, file.getFileName(), text));
						modelListBox.addItem(file.getFileName(),""+id);
						//controler.setValue(KEY_MODEL_SELECTION, id);
					}
				});
				reader.readAsText(file,"utf-8");
			}
		});
		
		modelControler=new StorageDataList(controler, "MODEL");
		modelSelection = new Label();
		add(modelSelection);

		modelListBox = new ListBox();
		modelListBox.setWidth("200px");
		add(modelListBox);
		modelListBox.setVisibleItemCount(5);
		// read from
		String modelText = PoseEditorBundles.INSTANCE.modelNames().getText();
		String[][] mapV = ValueUtils.csvToArray(modelText);
		for (int i = 0; i < mapV.length; i++) {
			if (mapV[i].length == 2) {
				String filePath = mapV[i][0];
				String id = mapV[i][1];
				String fileName = getFileNameWithoutExtension(filePath);
				presetModelMap.put(Integer.parseInt(id), filePath);
				modelListBox.addItem(fileName, id);
			}
		}

		modelListBox.addChangeHandler(new ChangeHandler() {

			@Override
			public void onChange(ChangeEvent event) {
				updateButtons();
			}
		});
		
		

		List<HeaderAndValue> models=modelControler.getDataList();
		for(int i=0;i<models.size();i++){
			modelListBox.addItem(models.get(i).getHeader(),""+models.get(i).getId());
		}
		
		int listBoxSelection=controler.getValue(KEY_MODEL_SELECTION, 0);//default bone
		
		if(listBoxSelection>=modelListBox.getItemCount()){
			LogUtils.log("warn model index is invalid:"+listBoxSelection);
			listBoxSelection=0;
		}
		modelListBox.setSelectedIndex(listBoxSelection);
		
		
		HorizontalPanel buttons = new HorizontalPanel();
		add(buttons);
		Button loadBt = new Button("Load");
		buttons.add(loadBt);
		loadBt.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				
				loadModelByListIndex(modelListBox
						.getSelectedIndex());
			}
		});
		removeBt = new Button("Remove");
		buttons.add(removeBt);
		removeBt.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				
				removeModelByListIndex(modelListBox
						.getSelectedIndex());
			}
		});
		// loadFrom store

		
		
		// list.addI
		// load
		// remove
		// export
	}
	protected void updateButtons() {
		//preset cant remove
		String id = modelListBox.getValue(modelListBox.getSelectedIndex());
		final int idIndex = Integer.parseInt(id);
		if(idIndex<0){
			removeBt.setEnabled(false);
		}else{
			removeBt.setEnabled(true);
		}
	}

	public void loadSelectionModel(){
		loadModelByListIndex(modelListBox.getSelectedIndex());
	}
	
	private void removeModelByListIndex(final int index) {
		String id = modelListBox.getValue(index);
		final String modelName=modelListBox.getItemText(index);
		
		LogUtils.log(id+","+modelName);
		final int idIndex = Integer.parseInt(id);
		if (idIndex < 0) {
			//never calld;
		} else {
			
			modelControler.clearData(idIndex);
			int boxIndex=modelListBox.getSelectedIndex();
			modelListBox.removeItem(boxIndex);
			modelListBox.setSelectedIndex(boxIndex-1);
			
			updateButtons();
			loadSelectionModel();
		}
	}

	private void loadModelByListIndex(final int index) {
		String id = modelListBox.getValue(index);
		final String modelName=modelListBox.getItemText(index);
		
		LogUtils.log(id+","+modelName);
		final int idIndex = Integer.parseInt(id);
		if (idIndex < 0) {
			String path = presetModelMap.get(idIndex);
			if (path != null) {

				RequestBuilder builder = new RequestBuilder(
						RequestBuilder.GET, URL.encode(path));

				try {
					builder.sendRequest(null, new RequestCallback() {

						@Override
						public void onResponseReceived(Request request,
								Response response) {
							
							loadModel(new HeaderAndValue(idIndex, modelName, response.getText()));
							controler.setValue(KEY_MODEL_SELECTION, index);
						}

						@Override
						public void onError(Request request,
								Throwable exception) {
							Window.alert("load faild:");
						}
					});
				} catch (RequestException e) {
					LogUtils.log(e.getMessage());
					e.printStackTrace();
				}
			} else {
				LogUtils.log("preset model not found" + idIndex);
			}
		} else {
			// load from cache
			HeaderAndValue hv=modelControler.getDataValue(idIndex);
			
			
			loadModel(hv);
			controler.setValue(KEY_MODEL_SELECTION, index);
		}
	}
	
	private void loadModel(HeaderAndValue value) {
		listener.modelChanged(value);
		modelSelection.setText("Selection:"+value.getHeader());
	}

	public String getFileNameWithoutExtension(String name) {
		int start = name.lastIndexOf("/");
		if (start == -1) {
			start = 0;
		} else {
			start++;
		}
		int end = name.lastIndexOf(".");
		if (end == -1) {
			end = name.length();
		}
		return name.substring(start, end);
	}

	public interface PreferenceListener {
		public void modelChanged(HeaderAndValue model);

		public void textureChanged(HeaderAndValue texture);
	}
}