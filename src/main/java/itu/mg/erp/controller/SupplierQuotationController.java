package itu.mg.erp.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import itu.mg.erp.features.request.QuotationSatusUpdateRequest;
import itu.mg.erp.features.request.QuotationUpdateRequest;
import itu.mg.erp.response.ERPNextResourceResponse;
import itu.mg.erp.response.ERPNextResourceSingleResponse;
import itu.mg.erp.service.SupplierQuotationService;
import itu.mg.erp.service.SupplierService;

@Controller
@RequestMapping("/quotation")
public class SupplierQuotationController {

    private final SupplierQuotationService _supplierService;
    private final SupplierService _supplierService2;

    public SupplierQuotationController(SupplierQuotationService supplierService, SupplierService supplierService2) {
        this._supplierService = supplierService;
        this._supplierService2 = supplierService2;

    }

    @GetMapping("/{name}")
    public String getListSuppliers(
        @PathVariable("name") String name,
        Model model
    ) {
        ERPNextResourceResponse data = _supplierService.getListSupplierQuotation(name);
        model.addAttribute("quotations", data.getData());
        return "app/supplier-quotations-list"; 
    }

    @GetMapping("/edit-form/{name}")
    public String editForm(@PathVariable("name") String name, Model model) {
        ERPNextResourceSingleResponse data = _supplierService.getListSupplierQuotationItem(name);
        Map<String, Object> dataMap = data.getData();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) dataMap.get("items");
        model.addAttribute("name", name);
        model.addAttribute("items", items);
        return "app/supplier-quotations-item-form";
    }

    @GetMapping("/form")
    public String FormSupplierQuotation(Model model) {
        ERPNextResourceResponse items = _supplierService.getListItems();
        ERPNextResourceResponse uoms = _supplierService.getListUOMs();
        ERPNextResourceResponse warehouses = _supplierService.getListWarehouses();
        ERPNextResourceResponse suppliers = _supplierService2.getListSupplier();
        model.addAttribute("items", items.getData());
        model.addAttribute("uoms", uoms.getData());
        model.addAttribute("warehouses", warehouses.getData());
        model.addAttribute("suppliers", suppliers.getData());
        return "app/supplier-quotations-form";
    }

    @GetMapping("/item/{name}")
    public String getItems(@PathVariable("name") String name, Model model) {
        ERPNextResourceSingleResponse data = _supplierService.getListSupplierQuotationItem(name);
        Map<String, Object> dataMap = data.getData();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) dataMap.get("items");
        model.addAttribute("name", name);
        model.addAttribute("items", items);
        return "app/supplier-quotations-item-list1";
    }

    @PostMapping("/update")
    public String updateRates(@RequestParam("nom") String name, @RequestParam Map<String, String> allParams) {
        QuotationSatusUpdateRequest updateQstatusRequest = new QuotationSatusUpdateRequest();
        updateQstatusRequest.setDocstatus(1);
        allParams.forEach((key, value) -> {
            if (!"nom".equals(key)) { // Ignorer la clé 'nom'
                QuotationUpdateRequest updateRequest = new QuotationUpdateRequest();
                updateRequest.setRate(Double.parseDouble(value));
                updateRequest.setDocstatus(1);
                _supplierService.updateRate(key, updateRequest);
            }
        });
        _supplierService.updateStatus(name, updateQstatusRequest);
    
        return "redirect:/suppliers"; 
    }

    // @PostMapping("/insert")
    // public String insert(
    //     @RequestParam("supplier") String supplier,
    //     @RequestParam("transaction_date") String date,
    //     @RequestParam("item")          List<String> itemCodes,
    //     @RequestParam("qty")           List<Integer> qtys,
    //     @RequestParam("rate")          List<Double> rates,
    //     @RequestParam("uom")           List<String> uoms,
    //     @RequestParam("warehouse")     List<String> warehouses
    // ) {
    //     Map<String, Object> param = new HashMap<>();
    //     param.put("supplier", supplier);
    //     param.put("transaction_date", date);

    //     List<Map<String, Object>> items = new ArrayList<>();

    //     int n = itemCodes.size(); 
    //     for (int i = 0; i < n; i++) {
    //         Map<String, Object> line = new HashMap<>();
    //         line.put("item_code", itemCodes.get(i));
    //         line.put("qty",        qtys.get(i));
    //         line.put("rate",       rates.get(i));
    //         line.put("uom",        uoms.get(i));
    //         line.put("warehouse",  warehouses.get(i));
    //         items.add(line);
    //     }

    //     param.put("items", items);

    //     _supplierService.insertQuotation(param);

    //     return "redirect:/suppliers";

    // }

    @PostMapping("/insert")
    public String insert(@RequestParam("supplier") String supplier, 
                         @RequestParam("item") String itemCode, 
                         @RequestParam("uom") String uom, 
                         @RequestParam("warehouse") String warehouse, 
                         @RequestParam("qty") Integer qty, 
                         @RequestParam("rate") Double rate, 
                         @RequestParam("transaction_date") String date)  {
        Map<String, Object> param = new HashMap<>();
        param.put("supplier", supplier);
        param.put("transaction_date", date);
        List<Map<String, Object>> items = new ArrayList<>();
        Map<String, Object> item = new HashMap<>();
        item.put("item_code", itemCode);
        item.put("uom", uom);
        item.put("warehouse", warehouse);
        item.put("qty", qty);
        item.put("rate", rate);
        items.add(item);
        param.put("items", items);
        _supplierService.insertQuotation(param);    
        return "redirect:/suppliers"; 
    }
    
}

