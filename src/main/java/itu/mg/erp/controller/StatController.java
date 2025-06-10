package itu.mg.erp.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import itu.mg.erp.response.ERPNextResourceResponse;
import itu.mg.erp.response.ERPNextResourceSingleResponse;
import itu.mg.erp.service.EmployeeService;
import itu.mg.erp.service.StatService;

@Controller
@RequestMapping("/stat")
public class StatController {

    private final StatService _statService;

    public StatController(StatService _statService) {
        this._statService = _statService;
    }

    @GetMapping
    public String getStatistic(
            Model model) {
        ERPNextResourceResponse response = _statService.getAllSalarySlips();
        model.addAttribute("salaryslip", response.getData());
        return "app/statistic";
    }

    @GetMapping("/salaryslips")
    public String showSalarySlips(@RequestParam String year, @RequestParam String month, Model model) throws JsonProcessingException {

        // Étape 1 : liste des salary slips du mois
        ERPNextResourceResponse slipsResponse = _statService.getSalaryYM(year, month);
        System.out.println("Réponse de getSalaryYM: " + slipsResponse);

        List<Map<String, Object>> detailedSlips = new ArrayList<>();

        if (slipsResponse != null && slipsResponse.getData() != null) {
            List<Map<String, Object>> slips = (List<Map<String, Object>>) slipsResponse.getData();
            System.out.println("Nombre de salary slips reçus: " + slips.size());

            for (Map<String, Object> slip : slips) {
                String name = (String) slip.get("name");
                if (name != null) {
                    ERPNextResourceSingleResponse detailResponse = _statService.getSalarySplit(name);

                    if (detailResponse != null && detailResponse.getData() != null) {
                        Map<String, Object> slipDetail = (Map<String, Object>) detailResponse.getData();
                        detailedSlips.add(slipDetail);
                    } else {
                        System.out.println("Aucun détail trouvé pour " + name);
                    }
                } else {
                    System.out.println("Salary slip sans nom trouvé.");
                }
            }
        } else {
            System.out.println("Aucune donnée reçue de getSalaryYM ou réponse nulle.");
        }

        System.out.println("Nombre total de salary slips détaillés à envoyer à la vue: " + detailedSlips.size());

        // On envoie la liste détaillée à la vue
        model.addAttribute("salarySlips", detailedSlips);
        model.addAttribute("year", year);
        model.addAttribute("month", month);

        return "app/salarymonthpage"; // nom de ta page .jsp ou .html
    }

}
