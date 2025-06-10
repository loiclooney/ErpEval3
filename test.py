def process_salary_import(file3):
    
    content3 = file3.read().decode("utf-8")
    csv_reader3 = csv.DictReader(io.StringIO(content3))


    for row in csv_reader3:
        try:
            # Vérification de l'employé
            employee = row.get("Ref Employe", "").strip()
            if not employee:
                frappe.throw(_("Employee not found: {0}").format(row["Ref Employe"]))

            # Vérification de la structure salariale
            salary_structure = salary_structures.get(row["Salaire"])
            if not salary_structure:
                frappe.throw(_("Salary structure not found: {0}").format(row["Salaire"]))

            # Création du Salary Component s’il est spécifié
            if row.get("Nom composant") and row.get("Valeur composant"):
                try:
                    account_name = frappe.db.get_value("Company", company, "default_payroll_payable_account")
                    comp_type_map = {
                        "earning": "Earning", "Earning": "Earning",
                        "deduction": "Deduction", "Deduction": "Deduction"
                    }
                    comp_type = comp_type_map.get(row.get("Type composant"), None)
                    if not comp_type:
                        frappe.throw(_("Invalid salary component type: {0}").format(row.get("Type composant")))

                    if not frappe.db.exists("Salary Component", row["Nom composant"]):
                        comp = frappe.new_doc("Salary Component")
                        comp.salary_component = row["Nom composant"]
                        comp.salary_component_abbr = row.get("Abréviation", row["Nom composant"][:3].upper())
                        comp.type = comp_type
                        comp.formula = row["Valeur composant"]
                        comp.remove_if_zero_valued = 1
                        comp.depends_on_payment_days = 0
                        comp.append("accounts", {
                            "company": company,
                            "account": account_name
                        })
                        comp.insert(ignore_permissions=True)
                        stats.created_documents += 1
                        stats.created_salary_components += 1
                        results["created_components"].append(comp.name)
                except Exception as e:
                    stats.errors.append(str(e))
                    stats.log_error(f"Error creating Salary Component {row.get('Nom composant')}: {str(e)}")
                    frappe.throw(_("Error creating Salary Component {0}: {1}").format(row.get("Nom composant"), str(e)))

            # Création de Salary Structure Assignment
            from_date = parseDate(row["Mois"])
            ssa = frappe.new_doc("Salary Structure Assignment")
            ssa.employee = employee.name
            ssa.salary_structure = salary_structure.name
            ssa.from_date = from_date
            ssa.base = row["Salaire Base"]
            ssa.insert(ignore_permissions=True)
            ssa.submit()
            results["assignments"].append(ssa)
            assignments_by_date[ssa.from_date].append(ssa)

            stats.created_documents += 1
            stats.created_salary_structure_assignment += 1

        except Exception as e:
            stats.errors.append(str(e))
            frappe.throw(_("Error processing row for employee {0}: {1}").format(row["Ref Employe"], str(e)))

    # Création des Payroll Entries par date
    for from_date, assignments in assignments_by_date.items():
        try:
            first = assignments[0]

            def get_field(obj, field):
                if hasattr(obj, "get"):
                    return obj.get(field)
                return getattr(obj, field, None)

            # Date parsing
            if isinstance(from_date, str):
                dt = datetime.strptime(from_date, "%Y-%m-%d")
                start_date = from_date
            elif hasattr(from_date, "strftime"):
                dt = from_date
                start_date = dt.strftime("%Y-%m-%d")
            else:
                raise ValueError(f"from_date {from_date} is not valid")

            end_date = dt.replace(day=calendar.monthrange(dt.year, dt.month)[1]).strftime("%Y-%m-%d")

            payroll_entry = frappe.new_doc("Payroll Entry")
            payroll_entry.posting_date = start_date
            payroll_entry.company = get_field(first, "company") or company
            payroll_entry.currency = get_field(first, "currency") or "MGA"
            payroll_entry.payroll_payable_account = get_field(first, "payroll_payable_account") or frappe.db.get_value("Company", company, "default_payroll_payable_account")
            payroll_entry.payroll_frequency = "Monthly"
            payroll_entry.start_date = start_date
            payroll_entry.end_date = end_date
            payroll_entry.exchange_rate = 1

            cost_center = get_field(first, "cost_center")
            if cost_center:
                payroll_entry.cost_center = cost_center

            added_employees = set()
            for ssa in assignments:
                emp = get_field(ssa, "employee")
                emp_name = get_field(ssa, "employee_name") or frappe.db.get_value("Employee", emp, "employee_name")
                if emp and emp not in added_employees:
                    payroll_entry.append("employees", {
                        "employee": emp,
                        "employee_name": emp_name
                    })
                    added_employees.add(emp)

            payroll_entry.number_of_employees = len(added_employees)
            payroll_entry.insert(ignore_permissions=True)
            payroll_entry.submit()

            salary_slips = payroll_entry.get_sal_slip_list(ss_status=0)
            submit_salary_slips_for_employees(payroll_entry, salary_slips)

            for slip in salary_slips:
                stats.created_salary_slip += 1

            results["payroll_entries"].append(payroll_entry)
            stats.created_documents += 1
            stats.created_payroll_entry += 1

        except Exception as e:
            stats.errors.append(str(e))
            frappe.throw(_("Error creating payroll entry for {0}: {1}").format(from_date, str(e)))

    return results
