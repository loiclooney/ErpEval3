import frappe, csv, io, random
import pycountry
from frappe.utils import getdate, nowdate, add_days
from frappe.database import get_db
from werkzeug.datastructures import FileStorage
from frappe.utils.data import now_datetime

@frappe.whitelist()
def reset_db():
    # Liste de tables à exclure
    protected = [
        "__Auth",
        "__global_search",
        "__user_settings",
        "__linked_with",
        "tabSessions",
        "tabVersion",
        "tabDocType",
        "tabDocField",
        "tabDocPerm",
        "tabCustom Field",
        "tabCustom DocPerm",
        "tabCustomize Form",
        "tabModule Def",
        "tabDefaultValue",
        "tabSingles",
        "tabCountry",
        "tabCurrency",
        "tabLanguage",
        "tabTime Zone",
        "tabUser",
        "tabRole",
        "tabHas Role",
        "tabUser Permission",
        "tabUser Type",
        "tabUser Type Module",
        "tabCompany",
        "tabFile",
        "tabReport",
        "tabPrint Format",
        "tabLetter Head",
        "tabEmail Account",
        "tabNotification Settings",
        "tabSystem Settings",
        "tabWebsite Settings",
        "tabError Log",
        "tabCommunication",
        "tabComment",
        "tabInstalled Application",
        "tabModule Profile",
        "tabBlock Module"
    ]
    
    # Récupère toutes les tables tab*
    tables = frappe.db.sql_list("SHOW TABLES LIKE 'tab%%'")
    for t in tables:
        if t not in protected:
            frappe.db.sql(f"TRUNCATE `{t}`")
    frappe.db.commit()
    
@frappe.whitelist()
def delete_specific_tables():
    # Liste des tables à supprimer complètement
    to_delete = [
        "tabSupplier",
        "tabItem",
        "tabItem Price",
        "tabMaterial Request",
        "tabMaterial Request Item",
        "tabSupplier Quotation",
        "tabSupplier Quotation Item",
        "tabPurchase Order",
        "tabPurchase Order Item",
        "tabRequest for Quotation",
        "tabRequest for Quotation Item",
        "tabPurchase Invoice",
        "tabPurchase Invoice Item",
        "tabPurchase Invoice Advance",
        "tabPayment Entry",
        "tabGL Entry",
        "tabPayment Entry Reference",
        "tabSales Invoice",
        "tabSales Invoice item",
        "tabSales Order",
        "tabSales Order Item",
        "tabPayment Ledger Entry",
        "tabRequest for Quotation",
        "tabRequest for Quotation Item",
        "tabRequest for Quotation Supplier"
    ]
    
    for t in to_delete:
        # Vérifier que la table existe avant de la supprimer
        exists = frappe.db.sql_list(f"SHOW TABLES LIKE '{t}'")
        if exists:
            frappe.db.sql(f"TRUNCATE TABLE `{t}`")
        
# Liste manuelle de correspondances pour les pays et leurs codes ISO
MANUAL_COUNTRY_ISO_MAP = {
    "USA": "US",
    "United States": "US",
    "United States of America": "US",
    "UK": "GB",
    "Great Britain": "GB",
    "England": "GB",
    "South Korea": "KR",
    "Korea": "KR",
    "North Korea": "KP",
    # Ajoutez d'autres correspondances si nécessaire
}

from collections import defaultdict

@frappe.whitelist()
def import_csv():
    """
    Import multiple CSV files for Purchase process with fallback generation for missing linked values.
    Ensures required fields are set and correctly links RFQ suppliers.
    """
    files = frappe.local.request.files.getlist('files')
    if not files:
        frappe.throw("Aucun fichier reçu")

    csvs = {fs.filename.rsplit('.', 1)[0]: fs for fs in files}
    imported = []
    mr_map = {}
    rfq_info_list = []  # store tuples (rfq_name, company)

    try:
        # 1. Suppliers
        if "Supplier" in csvs:
            for row in _read_csv(csvs["Supplier"]):
                country = row.get("country")
                if not country or not frappe.db.exists("Country", country):
                    if country:  # Si un pays est spécifié mais n'existe pas
                        country_code, country_name = get_iso_code_and_name(country)
                        if not country_code:  # Si aucune correspondance, vérifier dans la liste manuelle
                            country_code = MANUAL_COUNTRY_ISO_MAP.get(country)
                            if not country_code:  # Si toujours aucune correspondance, générer un code aléatoire
                                country_code = generate_custom_iso_code(country)
                                frappe.msgprint(f"Le pays '{country}' n'a pas de correspondance valide. Un code personnalisé '{country_code}' sera utilisé.")
                            country_name = country
                        # Conversion explicite des codes ISO à trois caractères en deux caractères
                        if len(country_code) == 3:
                            country_code = convert_iso_alpha3_to_alpha2(country_code)
                        # Validation explicite du code ISO
                        if not is_valid_iso_code(country_code):
                            frappe.throw(f"Le code ISO '{country_code}' pour le pays '{country}' n'est pas valide.")
                        frappe.get_doc({
                            "doctype": "Country",
                            "country_name": country_name,
                            "code": country_code
                        }).insert(ignore_permissions=True)
                    else:  # Sinon, choisir un pays aléatoire
                        country = _random_existing("Country")
                supplier = frappe.get_doc({
                    "doctype": "Supplier",
                    "supplier_name": row.get("supplier_name") or f"Supplier-{random.randint(1000,9999)}",
                    "supplier_type": row.get("type") or "Company",
                    "country": country
                })
                supplier.insert(ignore_permissions=True, ignore_links=True)
                imported.append(f"Supplier@{supplier.name}")
        frappe.db.commit()

        # 2. Material Requests
        default_company = frappe.db.get_default("Company")
        if "Material Request" in csvs:
            grouped_rows = defaultdict(list)
            for row in _read_csv(csvs["Material Request"]):
                grouped_rows[row.get("ref")].append(row)

            for ref, rows in grouped_rows.items():
                company = rows[0].get("company") if rows[0].get("company") and frappe.db.exists("Company", rows[0].get("company")) else default_company
                mr = frappe.get_doc({
                    "doctype": "Material Request",
                    "company": company,
                    "transaction_date": getdate(rows[0].get("date")),
                    "schedule_date": getdate(rows[0].get("required_by")),
                    "purpose": rows[0].get("purpose") or "Purchase",
                    "docstatus": 1
                })

                for row in rows:
                    item_code = row.get("item_name")
                    if not item_code or not frappe.db.exists("Item", item_code):
                        item_code = create_item_from_csv(row)
                    item_group = row.get("item_groupe")
                    if not item_group or not frappe.db.exists("Item Group", item_group):
                        item_group = create_item_group(item_group or "Default Group")
                    warehouse = row.get("target_warehouse")
                    if not warehouse or not frappe.db.exists("Warehouse", warehouse) or not _warehouse_belongs_to_company(warehouse, company):
                        if warehouse:
                            warehouse_name = f"{warehouse} - {frappe.db.get_value('Company', company, 'abbr')}"
                            if not frappe.db.exists("Warehouse", warehouse_name):
                                frappe.get_doc({"doctype": "Warehouse", "warehouse_name": warehouse, "company": company}).insert(ignore_permissions=True)
                            warehouse = warehouse_name
                        else:
                            warehouse = _random_warehouse(company)

                    mr.append("items", {
                        "item_code": item_code,
                        "item_group": item_group,
                        "qty": float(row.get("quantity")) if row.get("quantity") else random.randint(1, 10),
                        "warehouse": warehouse
                    })

                mr.insert(ignore_permissions=True, ignore_links=True)
                mr_map[ref] = {'name': mr.name, 'company': company, 'schedule_date': getdate(rows[0].get("required_by"))}
                imported.append(f"Material Request@{mr.name}")
        frappe.db.commit()

        # 3. Requests for Quotation
        if "Request for Quotation" in csvs:
            rfq_map = {}  # Map to track RFQs by Material Request reference
            for row in _read_csv(csvs["Request for Quotation"]):
                ref = row.get("ref_request_quotation")
                mr_info = mr_map.get(ref)
                if not mr_info:
                    frappe.throw(f"Réf MR inconnue: {ref}")
                mr_name = mr_info['name']
                company = mr_info['company']
                schedule_date = mr_info['schedule_date']
                supplier_name = row.get("supplier")
                if not supplier_name or not frappe.db.exists("Supplier", supplier_name):
                    supplier_name = _random_existing("Supplier")

                # Check if an RFQ already exists for this Material Request
                if mr_name not in rfq_map:
                    rfq = frappe.get_doc({
                        "doctype": "Request for Quotation",
                        "material_request": mr_name,
                        "transaction_date": nowdate(),
                        "schedule_date": schedule_date,  # Use the schedule date from Material Request
                        "status": row.get("status") or "Submitted",
                        "message_for_supplier": row.get("message_for_supplier") or "Merci de fournir votre devis.",
                        "docstatus": 1
                    })

                    # Add items from the Material Request to the RFQ
                    mr_doc = frappe.get_doc("Material Request", mr_name)
                    for item in mr_doc.items or []:
                        uom = frappe.db.get_value("Item", item.item_code, "stock_uom") or _random_existing("UOM")
                        conv = frappe.db.get_value("UOM Conversion Detail", {"parent": uom}, "conversion_factor") or 1
                        warehouse = item.warehouse if _warehouse_belongs_to_company(item.warehouse, company) else _random_warehouse(company)
                        rfq.append("items", {
                            "item_code": item.item_code,
                            "qty": item.qty,
                            "uom": uom,
                            "schedule_date": schedule_date,
                            "conversion_factor": conv,
                            "warehouse": warehouse
                        })

                    rfq.insert(ignore_permissions=True, ignore_links=True)
                    rfq_map[mr_name] = rfq.name  # Map the RFQ to the Material Request
                    rfq_info_list.append({'name': rfq.name, 'company': company})
                    imported.append(f"Request for Quotation@{rfq.name}")

                # Add the supplier to the existing RFQ
                rfq_name = rfq_map[mr_name]
                rfq_doc = frappe.get_doc("Request for Quotation", rfq_name)
                if not any(s.supplier == supplier_name for s in rfq_doc.suppliers):
                    rfq_doc.append("suppliers", {"supplier": supplier_name, "status": "To Receive"})
                rfq_doc.save(ignore_permissions=True)

        frappe.db.commit()

        # 4. Supplier Quotations
        for info in rfq_info_list:
            rfq_name = info['name']
            company = info['company']
            rfq_doc = frappe.get_doc("Request for Quotation", rfq_name)
            # pick first linked supplier
            supplier_name = rfq_doc.suppliers[0].supplier if rfq_doc.suppliers else _random_existing("Supplier")
            sq = frappe.new_doc("Supplier Quotation")
            sq.supplier = supplier_name
            sq.request_for_quotation = rfq_name
            sq.transaction_date = nowdate()
            sq.valid_till = add_days(nowdate(), random.randint(1,30))
            for item in rfq_doc.items or []:
                warehouse = item.warehouse if _warehouse_belongs_to_company(item.warehouse, company) else _random_warehouse(company)
                sq.append("items", {
                    "item_code": item.item_code,
                    "qty": item.qty,
                    "uom": item.uom,
                    "conversion_factor": item.conversion_factor,
                    "rate": _get_rate(item.item_code),
                    "warehouse": warehouse
                })
            if not sq.items:
                rand_item = _random_existing("Item")
                uom = frappe.db.get_value("Item", rand_item, "stock_uom") or _random_existing("UOM")
                warehouse = _random_warehouse(company)
                sq.append("items", {
                    "item_code": rand_item,
                    "qty": random.randint(1,5),
                    "uom": uom,
                    "conversion_factor": 1,
                    "rate": _get_rate(rand_item),
                    "warehouse": warehouse
                })
            sq.insert(ignore_permissions=True, ignore_links=True)
            imported.append(f"Supplier Quotation@{sq.name}")
        frappe.db.commit()

        frappe.msgprint(f"Import réussi pour {len(imported)} documents: {', '.join(imported)}")
        return {"status": "success", "imported": imported}
    except Exception as e:
        frappe.db.rollback()
        frappe.throw(f"Import annulé à cause d’une erreur : {e}")

def create_item_group(item_group_name):
    """
    Crée un groupe d'articles (Item Group) si celui-ci n'existe pas.
    """
    item_group = frappe.get_doc({
        "doctype": "Item Group",
        "item_group_name": item_group_name,
        "parent_item_group": "All Item Groups",
        "is_group": 0
    })
    item_group.insert(ignore_permissions=True, ignore_links=True)
    return item_group.name

def _read_csv(file_storage):
    file_storage.stream.seek(0)
    return list(csv.DictReader(io.StringIO(file_storage.stream.read().decode('utf-8'))))

def _warehouse_belongs_to_company(warehouse, company):
    return frappe.db.exists("Warehouse", {"name": warehouse, "company": company})

def _random_warehouse(company):
    wh = frappe.get_all("Warehouse", filters={"company": company}, fields=["name"], limit=1, order_by="RAND()")
    if wh:
        return wh[0].name
    return _random_existing("Warehouse")

def _random_existing(doctype):
    if doctype == "Item":
        items = frappe.get_all("Item", fields=["name"], limit=1, order_by="RAND()")
        if items:
            return items[0].name
        item_group = _random_existing("Item Group")
        uom = frappe.get_all("UOM", fields=["name"], limit=1, order_by="RAND()")
        uom = uom[0].name if uom else "Nos"
        code = f"Auto-ITEM-{random.randint(1000,9999)}"
        item = frappe.get_doc({"doctype":"Item","item_code":code,"item_name":code,"item_group":item_group,"stock_uom":uom})
        item.insert(ignore_permissions=True, ignore_links=True)
        return item.name
    res = frappe.get_all(doctype, fields=["name"], limit=1, order_by="RAND()")
    if res:
        return res[0].name
    doc = frappe.get_doc({"doctype": doctype, "name": f"Auto-{doctype}-{random.randint(1000,9999)}"})
    doc.insert(ignore_permissions=True, ignore_links=True)
    return doc.name

def _get_rate(item_code):
    rate = frappe.db.get_value("Item Price", {"item_code": item_code}, "price_list_rate")
    return rate or round(random.uniform(10, 100), 2)

def get_iso_code_from_name(country_name):
    """
    Tente de trouver un code ISO valide pour un pays donné en utilisant pycountry.
    Retourne le code ISO si trouvé, sinon None.
    """
    try:
        # Recherche exacte
        country = pycountry.countries.get(name=country_name)
        if country:
            return country.alpha_2
        # Recherche par similarité
        matches = [c for c in pycountry.countries if country_name.lower() in c.name.lower()]
        if matches:
            return matches[0].alpha_2
    except Exception:
        pass
    return None

def get_iso_code_and_name(country_name):
    """
    Tente de trouver un code ISO valide et le nom officiel pour un pays donné.
    Vérifie d'abord dans la liste manuelle, puis utilise pycountry.
    """
    # Vérification dans la liste manuelle
    if country_name in MANUAL_COUNTRY_ISO_MAP:
        return MANUAL_COUNTRY_ISO_MAP[country_name], country_name

    # Vérification avec pycountry
    try:
        country = pycountry.countries.get(name=country_name)
        if country:
            return country.alpha_2, country.name
        matches = [c for c in pycountry.countries if country_name.lower() in c.name.lower()]
        if matches:
            return matches[0].alpha_2, matches[0].name
    except Exception:
        pass
    return None, None

def generate_custom_iso_code(country_name):
    """
    Génère un code ISO personnalisé basé sur les 3 premières lettres du pays.
    Si le pays contient des espaces ou caractères spéciaux, ils sont supprimés.
    """
    sanitized_name = ''.join(e for e in country_name if e.isalnum())
    return sanitized_name[:3].upper() if sanitized_name else f"CUST-{random.randint(100, 999)}"

def convert_iso_alpha3_to_alpha2(alpha3_code):
    """
    Convertit un code ISO 3166 ALPHA-3 (3 caractères) en ALPHA-2 (2 caractères).
    Retourne le code ALPHA-2 si trouvé, sinon None.
    """
    try:
        country = pycountry.countries.get(alpha_3=alpha3_code)
        if country:
            return country.alpha_2
    except Exception:
        pass
    return None

def is_valid_iso_code(code):
    """
    Vérifie si un code ISO est valide (longueur de 2 caractères alphabétiques).
    """
    return len(code) == 2 and code.isalpha()

def create_item_from_csv(row):
    """
    Crée un produit (Item) à partir des données du CSV si le produit n'existe pas.
    """
    item_code = row.get("item_name") or f"Auto-ITEM-{random.randint(1000,9999)}"
    item_group = row.get("item_groupe") or create_item_group("Default Group")
    uom = row.get("uom") or "Nos"
    description = row.get("description") or f"Description for {item_code}"
    item = frappe.get_doc({
        "doctype": "Item",
        "item_code": item_code,
        "item_name": row.get("item_name") or item_code,
        "item_group": item_group,
        "stock_uom": uom,
        "description": description,
        "is_stock_item": 1,
        "is_purchase_item": 1
    })
    item.insert(ignore_permissions=True, ignore_links=True)
    return item.name