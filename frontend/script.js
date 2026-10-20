
const API_URL = "https://smartbank-backend-3j3e.onrender.com/api";

const $ = (id) => document.getElementById(id);

async function apiRequest(url, options = {}) {
    const response = await fetch(`${API_URL}${url}`, options);
    const data = await response.json();

    if (!response.ok) {
        throw new Error(data.error || "Request failed");
    }

    return data;
}

function showMessage(text, isError = false) {
    const message = $("message");

    if (message) {
        message.textContent = text;
        message.style.color = isError ? "#b42318" : "#287a48";
    }
}

function formData(formId) {
    const form = $(formId);
    return form ? new FormData(form) : null;
}

function jsonOptions(data) {
    return {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(data)
    };
}

function getValue(id) {
    return $(id)?.value.trim() || "";
}

async function loadAccounts() {
    try {
        const accounts = await apiRequest("/accounts");
        const table = $("accountsTable");

        if (table) {
            const tbody = table.querySelector("tbody") || table;
            tbody.innerHTML = "";

            accounts.forEach(account => {
                const row = document.createElement("tr");

                [
                    account.accountNumber,
                    account.name,
                    account.accountType,
                    `₹${Number(account.balance).toFixed(2)}`
                ].forEach(value => {
                    const cell = document.createElement("td");
                    cell.textContent = value;
                    row.appendChild(cell);
                });

                tbody.appendChild(row);
            });
        }

        if ($("totalAccounts")) {
            $("totalAccounts").textContent = accounts.length;
        }

        if ($("totalBalance")) {
            const total = accounts.reduce(
                (sum, account) => sum + Number(account.balance), 0
            );

            $("totalBalance").textContent = `₹${total.toFixed(2)}`;
        }
    } catch (error) {
        console.error("Loading accounts failed:", error);
    }
}

// Create Account
if ($("createForm")) {
    $("createForm").addEventListener("submit", async event => {
        event.preventDefault();
        showMessage("Creating account...");

        try {
            const data = await apiRequest(
                "/create-account",
                jsonOptions({
                    name: getValue("name"),
                    mobile: getValue("mobile"),
                    type: getValue("accountType"),
                    initialDeposit: Number(getValue("initialDeposit"))
                })
            );

            showMessage(
                `Account created successfully! Account Number: ${data.accountNumber}`
            );

            $("createForm").reset();
            await loadAccounts();
        } catch (error) {
            showMessage(error.message, true);
        }
    });
}

// Deposit and Withdraw
async function handleMoneyForm(formId, endpoint, amountId) {
    if (!$ (formId)) return;

    $(formId).addEventListener("submit", async event => {
        event.preventDefault();
        showMessage("Processing transaction...");

        try {
            const accountNumber = getValue(
                formId === "depositForm"
                    ? "depositAccount" : "withdrawAccount"
            );

            const amount = Number(getValue(amountId));

            const data = await apiRequest(
                endpoint,
                jsonOptions({ accountNumber, amount })
            );

            showMessage(
                `${data.message}. New balance: ₹${Number(data.balance).toFixed(2)}`
            );

            $(formId).reset();
            await loadAccounts();
        } catch (error) {
            showMessage(error.message, true);
        }
    });
}

handleMoneyForm("depositForm", "/deposit", "depositAmount");
handleMoneyForm("withdrawForm", "/withdraw", "withdrawAmount");

// Balance and account information
async function handleDetailsForm(formId, endpoint, resultId) {
    if (!$(formId)) return;

    $(formId).addEventListener("submit", async event => {
        event.preventDefault();

        const accountNumber = getValue(
            formId === "balanceForm" ? "balanceAccount" : "infoAccount"
        );

        try {
            const data = await apiRequest(
                `${endpoint}?accountNumber=${encodeURIComponent(accountNumber)}`
            );

            const result = $(resultId);
            if (result) {
                result.textContent =
                    `Account: ${data.accountNumber} | ` +
                    `Name: ${data.name} | Type: ${data.accountType} | ` +
                    `Balance: ₹${Number(data.balance).toFixed(2)}`;
            }
        } catch (error) {
            const result = $(resultId);
            if (result) result.textContent = error.message;
        }
    });
}

handleDetailsForm("balanceForm", "/balance", "balanceResult");
handleDetailsForm("infoForm", "/account-info", "infoResult");

// Passbook
if ($("passbookForm")) {
    $("passbookForm").addEventListener("submit", async event => {
        event.preventDefault();

        const accountNumber = getValue("passbookAccount");
        const fromDate = getValue("fromDate");
        const toDate = getValue("toDate");

        const result = $("passbookResult");
        if (result) result.textContent = "Loading passbook...";

        try {
            const query =
                `?accountNumber=${encodeURIComponent(accountNumber)}` +
                `&fromDate=${encodeURIComponent(fromDate)}` +
                `&toDate=${encodeURIComponent(toDate)}`;

            const data = await apiRequest(`/passbook${query}`);

            if (!result) return;

            if (!data.transactions.length) {
                result.textContent = "No transactions found for this period.";
                return;
            }

            const table = document.createElement("table");
            table.innerHTML =
                "<thead><tr><th>Date</th><th>Type</th>" +
                "<th>Amount</th><th>Balance</th></tr></thead>";

            const tbody = document.createElement("tbody");

            data.transactions.forEach(transaction => {
                const row = document.createElement("tr");

                [
                    new Date(transaction.dateTime).toLocaleString(),
                    transaction.type,
                    `₹${Number(transaction.amount).toFixed(2)}`,
                    `₹${Number(transaction.balanceAfter).toFixed(2)}`
                ].forEach(value => {
                    const cell = document.createElement("td");
                    cell.textContent = value;
                    row.appendChild(cell);
                });

                tbody.appendChild(row);
            });

            table.appendChild(tbody);
            result.replaceChildren(table);
        } catch (error) {
            if (result) result.textContent = error.message;
        }
    });
}

// Refresh Accounts
if ($("refreshAccounts")) {
    $("refreshAccounts").addEventListener("click", loadAccounts);
}

loadAccounts();
