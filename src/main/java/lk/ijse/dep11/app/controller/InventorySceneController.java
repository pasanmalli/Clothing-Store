package lk.ijse.dep11.app.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import lk.ijse.dep11.app.common.UserDetails;
import lk.ijse.dep11.app.common.WindowNavigation;
import lk.ijse.dep11.app.db.ItemDataAccess;
import lk.ijse.dep11.app.tm.Item;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;

public class InventorySceneController {
    public AnchorPane root;
    public ImageView imgProfileImage;
    public Label lblUserId;
    public Label lblUserName;
    public Button btnLogout;
    public ImageView imgViewLogo;
    public TextField txtItemCode;
    public TextField txtDescription;
    public TextField txtStock;
    public TextField txtUnitPrice;
    public TextField txtDiscount;
    public Button btnAdd;
    public Button btnDelete;
    public TableView<Item> tblItems;
    public TextField txtSearch;
    public Button btnNewItem;

    public void initialize() {
        final String PROFILE_IMAGE_NAME;
        if (UserDetails.getLoggedUser().getGender().equals("male"))
            PROFILE_IMAGE_NAME = "male-avatar.jpg";
        else
            PROFILE_IMAGE_NAME = "female-avatar.jpg";

        Platform.runLater(() -> {
            ImageView imageView = new ImageView(new Image(getClass().getResourceAsStream("/img/logout.png")));
            btnLogout.setGraphic(imageView);
            imgProfileImage.setImage(new Image(getClass().getResourceAsStream("/img/".concat(PROFILE_IMAGE_NAME))));
            lblUserId.setText(UserDetails.getLoggedUser().getId());
            lblUserName.setText(UserDetails.getLoggedUser().getFirstName());
        });

        String[] itemProperties = {"itemCode", "description", "qtyOnHand", "unitPrice"};
        for (int i = 0; i < itemProperties.length; i++) {
            tblItems.getColumns().get(i).setCellValueFactory(new PropertyValueFactory<>(itemProperties[i]));
        }

        Platform.runLater(() -> txtSearch.requestFocus());

        try {
            tblItems.getItems().addAll(ItemDataAccess.findItems(""));
        } catch (SQLException e) {
            showAlert("Unable to connect with the database.");
            e.printStackTrace();
        }

        txtSearch.textProperty().addListener((ov, prev, cur) -> {
            tblItems.getItems().clear();
            String query = cur.isEmpty() ? "" : cur.strip();
            try {
                tblItems.getItems().addAll(ItemDataAccess.findItems(query));
            } catch (SQLException e) {
                showAlert("Unable to connect with the database.");
                e.printStackTrace();
            }
        });

        btnDelete.setDisable(true);
        btnAdd.setDisable(true);
        tblItems.getSelectionModel().selectedItemProperty().addListener((ov, prev, cur) -> {
            if (cur != null) {
                txtItemCode.setText(cur.getItemCode());
                txtDescription.setText(cur.getDescription());
                txtStock.setText(String.valueOf(cur.getQtyOnHand()));
                txtUnitPrice.setText(String.valueOf(cur.getUnitPrice()));

                btnAdd.setText("UPDATE");
                textFieldsSetEditable();
                txtItemCode.setEditable(false);
                btnDelete.setDisable(false);
                btnAdd.setDisable(false);
            } else {
                cleanTextFields();
                txtItemCode.setEditable(true);
            }
        });
    }

    private void cleanTextFields() {
        for (TextField textField : new TextField[]{txtSearch, txtItemCode, txtDescription, txtStock, txtUnitPrice, txtDiscount}) {
            if (textField != null) {
                textField.clear();
            } else {
                System.err.println("TextField is null: " + textField);
            }
        }
    }

    public void imgViewLogoOnMouseClicked(MouseEvent mouseEvent) throws IOException {
        WindowNavigation.navigateToDashboard(root);
    }

    public void btnLogoutOnAction(ActionEvent actionEvent) {
        WindowNavigation.loggingOut(root);
    }

    public void btnAddOnAction(ActionEvent actionEvent) {
        try {
            if (isDataValid()) {
                Item newItem = new Item(
                        txtItemCode.getText().strip(),
                        txtDescription.getText().strip(),
                        Integer.parseInt(txtStock.getText().strip()),
                        new BigDecimal(Double.parseDouble(txtUnitPrice.getText().strip()))
                );

                if (btnAdd.getText().strip().equals("ADD")) {
                    ItemDataAccess.setItem(newItem);
                } else {
                    ItemDataAccess.updateItem(newItem);
                }
                btnNewItem.fire();
            }
        } catch (SQLException e) {
            showAlert("Unable to establish a database connection.");
            e.printStackTrace();
        }
    }

    private boolean isDataValid() throws SQLException {
        String itemCode = txtItemCode.getText().strip();
        if (!itemCode.matches("\\d+")) {
            showAlert("Item code format error.");
            txtItemCode.requestFocus();
            txtItemCode.selectAll();
            return false;
        } else if (btnAdd.getText().strip().equals("ADD") && ItemDataAccess.itemExist(itemCode)) {
            showAlert("Item already exists.");
            txtItemCode.requestFocus();
            txtItemCode.selectAll();
            tblItems.getItems().clear();
            tblItems.getItems().addAll(ItemDataAccess.findItems(itemCode));
            return false;
        } else if (txtDescription.getText().strip().length() <= 3) {
            showAlert("Description should be at least 3 characters long.");
            txtDescription.requestFocus();
            txtDescription.selectAll();
            return false;
        } else if (!(txtStock.getText().strip().matches("\\d+") && Double.parseDouble(txtStock.getText().strip()) > 0)) {
            showAlert("Stock should be a positive number.");
            txtStock.requestFocus();
            txtStock.selectAll();
            return false;
        } else if (!isPrice()) {
            showAlert("Price format error.");
            txtUnitPrice.requestFocus();
            txtUnitPrice.selectAll();
            return false;
        }
        return true;
    }

    private boolean isPrice() {
        try {
            Double price = Double.parseDouble(txtUnitPrice.getText().strip());
            return price > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public void btnDeleteOnAction(ActionEvent actionEvent) {
        Item selectedItem = tblItems.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            try {
                ItemDataAccess.deleteItem(selectedItem.getItemCode());
                btnNewItem.fire();
            } catch (SQLException e) {
                showAlert("Unable to establish a database connection.");
                e.printStackTrace();
            }
        }
    }

    public void btnNewItemOnAction(ActionEvent actionEvent) throws SQLException {
        if (areTextFieldsInitialized()) {
            cleanTextFields();
            btnAdd.setText("ADD");
            btnDelete.setDisable(true);
            textFieldsSetEditable();
            txtItemCode.requestFocus();
            tblItems.getItems().clear();
            tblItems.getItems().addAll(ItemDataAccess.findItems(""));
            btnAdd.setDisable(false);
        } else {
            System.err.println("One or more text fields are not initialized.");
        }
    }

    private boolean areTextFieldsInitialized() {
        return txtItemCode != null && txtDescription != null && txtStock != null &&
                txtUnitPrice != null && txtDiscount != null && txtSearch != null;
    }

    private void textFieldsSetEditable() {
        for (TextField textField : new TextField[]{txtItemCode, txtDescription, txtStock, txtUnitPrice, txtDiscount}) {
            if (textField != null) {
                textField.setEditable(true);
            }
        }
    }

    private void showAlert(String message) {
        new Alert(Alert.AlertType.ERROR, message).showAndWait();
    }
}
