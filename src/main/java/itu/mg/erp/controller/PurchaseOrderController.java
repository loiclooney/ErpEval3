package itu.mg.erp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import itu.mg.erp.response.ERPNextResourceResponse;
import itu.mg.erp.service.PurchaseOrderService;

@Controller
@RequestMapping("/purchase")
public class PurchaseOrderController {

    private final PurchaseOrderService _purchaseOrderService;

    public PurchaseOrderController(PurchaseOrderService supplierService) {
        this._purchaseOrderService = supplierService;
    }

    @GetMapping("/{name}")
    public String getListPurchaseOrder(
        @PathVariable("name") String name,
        Model model
    ) {
        ERPNextResourceResponse data = _purchaseOrderService.getListPurchaseOrderHave(name);
        ERPNextResourceResponse donnees = _purchaseOrderService.getListPurchaseOrderPayed(name);
        model.addAttribute("recus", data.getData());
        model.addAttribute("payes", donnees.getData());
        return "app/purchase-list"; 
    }
}

