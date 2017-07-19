package com.google.gwt.sample.stockwatcher.client;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.Random;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class StockWatcher implements EntryPoint {

	private static final int REFRESH_INTERVAL = 5000;
	private VerticalPanel mainPanel = new VerticalPanel();
	private FlexTable stocksFlexTable = new FlexTable();
	private HorizontalPanel addPanel = new HorizontalPanel();
	private TextBox newSymbolTextBox = new TextBox();
	private Button addStockButton = new Button("Add");
	private Label lastUpdatedLabel = new Label();
	private List<String> stocks = new ArrayList<String>();
	private StockPriceServiceAsync stockPriceSvc = GWT.create(StockPriceService.class);
	private Label errorMessageLabel = new Label();
	
	
	public void onModuleLoad() {

		stocksFlexTable.setText(0, 0, "Symbol");
		stocksFlexTable.setText(0, 1, "Price");
		stocksFlexTable.setText(0, 2, "Change");
		stocksFlexTable.setText(0, 3, "Remove");
		stocksFlexTable.getRowFormatter().addStyleName(0, "watchListHeader");
		stocksFlexTable.addStyleName("watchList");
		stocksFlexTable.getCellFormatter().addStyleName(0, 1, "watchListNumericColumn");
		stocksFlexTable.getCellFormatter().addStyleName(0, 2, "watchListNumericColumn");
		stocksFlexTable.getCellFormatter().addStyleName(0, 3, "watchListRemoveColumn");

		errorMessageLabel.setStyleName("errorMessage");
		errorMessageLabel.setVisible(false);
		addPanel.add(newSymbolTextBox);
		addPanel.add(addStockButton);

		mainPanel.add(errorMessageLabel);
		mainPanel.add(stocksFlexTable);
		mainPanel.add(addPanel);
		mainPanel.add(lastUpdatedLabel);
		

		RootPanel.get("stockList").add(mainPanel);

		newSymbolTextBox.setFocus(true);

		Timer refreshTimer = new Timer() {

			@Override
			public void run() {
				refreshWatchList();
			}

		};
		refreshTimer.scheduleRepeating(REFRESH_INTERVAL);

		addStockButton.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				addStock();
			}

		});

		newSymbolTextBox.addKeyDownHandler(new KeyDownHandler() {

			@Override
			public void onKeyDown(KeyDownEvent event) {
				if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
					addStock();
				}
			}
		});

	}

	private void addStock() {
		final String symbol = newSymbolTextBox.getText().toUpperCase().trim();
		if (!symbol.matches("^[0-9A-Z\\.]{1,10}$")) {
			Window.alert("'" + symbol + "' is not a valid symbol.");
			newSymbolTextBox.selectAll();
			return;
		}
		if (stocks.contains(symbol))
			return;

		int row = stocksFlexTable.getRowCount();
		stocks.add(symbol);
		stocksFlexTable.setText(row, 0, symbol);
		 stocksFlexTable.setWidget(row, 2, new Label());
		    stocksFlexTable.getCellFormatter().addStyleName(row, 1, "watchListNumericColumn");
		    stocksFlexTable.getCellFormatter().addStyleName(row, 2, "watchListNumericColumn");
		    stocksFlexTable.getCellFormatter().addStyleName(row, 3, "watchListRemoveColumn");
		
		Button removeStockButton = new Button("x");
		removeStockButton.addStyleDependentName("remove");
		removeStockButton.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				int removedIndex = stocks.indexOf(symbol);
				stocks.remove(removedIndex);
				stocksFlexTable.removeRow(removedIndex + 1);
			}
		});
		stocksFlexTable.setWidget(row, 3, removeStockButton);
		refreshWatchList();

		newSymbolTextBox.setText("");
	}

	protected void refreshWatchList() {
		if(stockPriceSvc == null){
			stockPriceSvc = GWT.create(StockPriceService.class);
		}
		AsyncCallback<StockPrice[]> callback = new AsyncCallback<StockPrice[]>(){

			@Override
			public void onFailure(Throwable caught) {
				String details = caught.getMessage();
				if(caught instanceof DelistedException){
					details = "Company '" + ((DelistedException)caught).getSymbol() + "' was delisted!";
				}
				errorMessageLabel.setText("Error: " + details);
				errorMessageLabel.setVisible(true);
				
			}

			@Override
			public void onSuccess(StockPrice[] result) {
				updateTable(result);
			}
			
		};
		
		stockPriceSvc.getPrices(stocks.toArray(new String[0]), callback);
	}

	private void updateTable(StockPrice[] prices) {
		for (int i = 0; i < prices.length; i++) {
			updateTable(prices[i]);
		}

		DateTimeFormat dateFormat = DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.DATE_TIME_MEDIUM);
		lastUpdatedLabel.setText("Last update: " + dateFormat.format(new Date()));
	
		errorMessageLabel.setVisible(false);
	}

	private void updateTable(StockPrice price) {
		if (!stocks.contains(price.getSymbol())) {
			return;
		}

		int row = stocks.indexOf(price.getSymbol()) + 1;
		String priceText = NumberFormat.getFormat("#,##0.00").format(price.getPrice());
		NumberFormat changeFormat = NumberFormat.getFormat("+#,##0.00;-#,##0.00");
		String changeText = changeFormat.format(price.getChange());
		String changePercentText = changeFormat.format(price.getChangePercent());

		stocksFlexTable.setText(row, 1, priceText);
		 Label changeWidget = (Label)stocksFlexTable.getWidget(row, 2);
		 changeWidget.setText(changeText + " (" + changePercentText + "%)");
		 String changeStyleName = "noChange";
		 if (price.getChangePercent() < -0.1f) {
		   changeStyleName = "negativeChange";
		 }
		 else if (price.getChangePercent() > 0.1f) {
		   changeStyleName = "positiveChange";
		 }

		 changeWidget.setStyleName(changeStyleName);
	}

}
