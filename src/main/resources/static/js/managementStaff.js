const STAFF_ROLES = ['WORKER', 'MANAGER', 'DRIVER', 'ANALYST'];
const isoDatePattern = /^\d{4}-\d{2}-\d{2}$/;

function qs(id) {
    return document.getElementById(id);
}

function escapeHtml(value) {
    return String(value ?? '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

function number(value) {
    return Number(value || 0).toLocaleString('en-GB');
}

function connectDatePicker(displayId, pickerId) {
    const display = qs(displayId);
    const picker = qs(pickerId);
    const button = document.querySelector(`[data-date-target="${pickerId}"]`);

    if (!display || !picker || !button) return;

    button.addEventListener('click', () => {
        picker.value = isoDatePattern.test(display.value) ? display.value : '';

        if (typeof picker.showPicker === 'function') {
            picker.showPicker();
        } else {
            picker.focus();
            picker.click();
        }
    });

    picker.addEventListener('change', () => {
        display.value = picker.value;
        display.focus();
    });

    display.addEventListener('input', () => {
        if (isoDatePattern.test(display.value) || display.value === '') {
            picker.value = display.value;
        }
    });
}

function setStatus(message, isError = false) {
    const status = qs('staff-status');
    status.textContent = message;
    status.classList.toggle('error', isError);
}

function showQueryMessage() {
    const params = new URLSearchParams(window.location.search);
    const error = params.get('error');
    const success = params.get('success');

    if (error === 'weak_password') {
        setStatus('Password must include uppercase, lowercase, number, and special character.', true);
    } else if (error === 'email_exists') {
        setStatus('A user with this email already exists.', true);
    } else if (error === 'missing_fields') {
        setStatus('Please complete all required fields.', true);
    } else if (success) {
        setStatus(`Account created successfully. New Staff ID: ${success}`);
    }
}

async function loadStaff() {
    setStatus('Loading staff accounts');

    try {
        const response = await fetch('/management/api/staff');

        if (!response.ok) {
            throw new Error(await response.text());
        }

        const staffRows = await response.json();
        renderStaff(staffRows);

        if (!new URLSearchParams(window.location.search).toString()) {
            setStatus('');
        } else {
            showQueryMessage();
        }
    } catch (error) {
        console.error(error);
        setStatus('Unable to load staff accounts.', true);
    }
}

function renderStaff(rows) {
    qs('staff-count').textContent = `${number(rows.length)} staff accounts`;
    const tbody = qs('staff-table-body');
    
    if (rows.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="empty-state">No staff accounts found.</td></tr>';
        return;
    }

    tbody.innerHTML = rows.map(row => `
        <tr>
            <td data-label="ID">#${row.userId}</td>
            <td data-label="Name">${escapeHtml(row.name)}</td>
            <td data-label="Staff ID">${escapeHtml(row.staffId)}</td>
            <td data-label="Email">${escapeHtml(row.email)}</td>
            <td data-label="Phone">${escapeHtml(row.phoneNumber)}</td>
            <td data-label="Role">
                <select class="role-select" data-original-role="${escapeHtml(row.role)}">
                    ${roleOptions(row.role)}
                </select>
            </td>
            <td data-label="Actions" class="action-cell">
                <button type="button" class="secondary-button save-role-btn" data-user-id="${row.userId}">Save Role</button>
                <button
                    type="button"
                    class="danger-button delete-staff-btn"
                    data-user-id="${row.userId}"
                    data-user-name="${escapeHtml(row.name)}"
                >
                    Delete
                </button>
            </td>
        </tr>
    `).join('');
}

function roleOptions(currentRole) {
    return STAFF_ROLES.map(role => `
        <option value="${role}" ${role === currentRole ? 'selected' : ''}>${role}</option>
    `).join('');
}

async function updateRole(button) {
    const row = button.closest('tr');
    const select = row.querySelector('.role-select');
    const userId = button.dataset.userId;
    const role = select.value;

    setStatus('Updating staff role');

    try {
        const response = await fetch(`/management/api/users/${userId}/role`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ role }),
        });
        const message = response.ok ? (await response.json()).message : await response.text();

        if (!response.ok) {
            throw new Error(message);
        }

        setStatus(message);
        await loadStaff();
    } catch (error) {
        console.error(error);
        setStatus(error.message || 'Unable to update staff role.', true);
    }
}

async function deleteStaff(button) {
    const userId = button.dataset.userId;
    const userName = button.dataset.userName;

    if (!window.confirm(`Delete staff account for ${userName}? This cannot be undone.`)) {
        return;
    }

    setStatus('Deleting staff account');

    try {
        const response = await fetch(`/management/api/users/${userId}`, { method: 'DELETE' });
        const message = response.ok ? (await response.json()).message : await response.text();

        if (!response.ok) {
            throw new Error(message);
        }

        setStatus(message);
        await loadStaff();
    } catch (error) {
        console.error(error);
        setStatus(error.message || 'Unable to delete staff account.', true);
    }
}

function validateCreateForm(event) {
    const form = qs('staff-create-form');
    let hasError = false;

    form.querySelectorAll('input, select').forEach(input => input.classList.remove('err'));
    form.querySelectorAll('.field-err').forEach(error => error.classList.remove('visible'));

    function requireField(id, errorId) {
        const field = qs(id);
        if (!field.value.trim()) {
            field.classList.add('err');
            qs(errorId).classList.add('visible');
            hasError = true;
        }
    }

    requireField('firstname', 'firstname-err');
    requireField('surname', 'surname-err');
    requireField('phone', 'phone-err');
    requireField('role', 'role-err');

    const email = qs('email');
    if (!email.value.includes('@') || email.value.length < 5) {
        email.classList.add('err');
        qs('email-err').classList.add('visible');
        hasError = true;
    }

    const password = qs('password');
    if (password.value.length < 8) {
        password.classList.add('err');
        qs('pass-err').classList.add('visible');
        hasError = true;
    }

    const dob = qs('dob');
    if (!isAdult(dob.value)) {
        dob.classList.add('err');
        qs('dob-err').classList.add('visible');
        hasError = true;
    }

    if (hasError) {
        event.preventDefault();
    }
}

function isAdult(dateValue) {
    if (!isoDatePattern.test(dateValue)) return false;

    const birthDate = new Date(dateValue);
    const today = new Date();
    let age = today.getFullYear() - birthDate.getFullYear();
    const monthOffset = today.getMonth() - birthDate.getMonth();

    if (monthOffset < 0 || (monthOffset === 0 && today.getDate() < birthDate.getDate())) {
        age--;
    }

    return age >= 18;
}

qs('staff-table-body').addEventListener('click', event => {
    const saveButton = event.target.closest('.save-role-btn');
    const deleteButton = event.target.closest('.delete-staff-btn');

    if (saveButton) {
        updateRole(saveButton);
    } else if (deleteButton) {
        deleteStaff(deleteButton);
    }
});

qs('staff-create-form').addEventListener('submit', validateCreateForm);
connectDatePicker('dob', 'dob-picker');
qs('show-pass').addEventListener('click', () => {
    const input = qs('password');
    const button = qs('show-pass');
    const showingPassword = input.type === 'text';

    input.type = showingPassword ? 'password' : 'text';
    button.textContent = showingPassword ? 'Show' : 'Hide';
});

showQueryMessage();
loadStaff();
