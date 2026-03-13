import json
import uuid
import urllib.request
import urllib.error


def req(method, url, body=None, headers=None):
    headers = headers or {}
    data = None
    if body is not None:
        data = json.dumps(body).encode("utf-8")
        headers = {**headers, "Content-Type": "application/json"}
    request = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            payload_raw = response.read().decode("utf-8")
            try:
                payload = json.loads(payload_raw) if payload_raw else None
            except Exception:
                payload = payload_raw
            return response.status, payload
    except urllib.error.HTTPError as error:
        payload_raw = error.read().decode("utf-8")
        try:
            payload = json.loads(payload_raw) if payload_raw else None
        except Exception:
            payload = payload_raw
        return error.code, payload


def matrix(base):
    base_auth = f"{base}/auth"
    uid = uuid.uuid4().hex[:8]
    email = f"{uid}@test.com"
    username = f"u{uid}"
    results = {}

    results["health"] = req("GET", f"{base_auth}/health")[0]

    status, body = req(
        "POST",
        f"{base_auth}/register",
        {
            "username": username,
            "email": email,
            "password": "Password1!",
            "fullName": "User",
        },
    )
    results["register_ok"] = status
    access = (body or {}).get("accessToken")
    refresh = (body or {}).get("refreshToken")

    results["register_duplicate"] = req(
        "POST",
        f"{base_auth}/register",
        {
            "username": username,
            "email": email,
            "password": "Password1!",
            "fullName": "Dup",
        },
    )[0]

    results["login_ok"] = req(
        "POST", f"{base_auth}/login", {"email": email, "password": "Password1!"}
    )[0]
    results["login_bad"] = req(
        "POST", f"{base_auth}/login", {"email": email, "password": "bad"}
    )[0]
    results["validate_ok"] = req(
        "POST", f"{base_auth}/validate", None, {"Authorization": f"Bearer {access}"}
    )[0]
    results["validate_missing"] = req("POST", f"{base_auth}/validate")[0]

    status, body = req("POST", f"{base_auth}/refresh", {"refreshToken": refresh})
    results["refresh_ok"] = status
    refresh2 = (body or {}).get("refreshToken")

    headers = {"Authorization": f"Bearer {access}"}
    results["users_me"] = req("GET", f"{base_auth}/users/me", None, headers)[0]
    results["profile_update"] = req(
        "PUT", f"{base_auth}/users/profile", {"fullName": "Updated"}, headers
    )[0]

    status, body = req(
        "POST",
        f"{base_auth}/users/addresses",
        {
            "street": "1 Main",
            "city": "Colombo",
            "postalCode": "10000",
            "isDefault": True,
        },
        headers,
    )
    results["address_create"] = status
    address_id = (body or {}).get("id")

    results["address_list"] = req("GET", f"{base_auth}/users/addresses", None, headers)[
        0
    ]

    if address_id:
        results["address_update"] = req(
            "PUT",
            f"{base_auth}/users/addresses/{address_id}",
            {
                "street": "2 Main",
                "city": "Kandy",
                "postalCode": "20000",
                "isDefault": False,
            },
            headers,
        )[0]
        results["address_delete"] = req(
            "DELETE", f"{base_auth}/users/addresses/{address_id}", None, headers
        )[0]

    results["change_password"] = req(
        "POST",
        f"{base_auth}/change-password",
        {"oldPassword": "Password1!", "newPassword": "NewPassword2!"},
        headers,
    )[0]

    status, body = req("POST", f"{base_auth}/forgot-password", {"email": email})
    results["forgot_password"] = status
    reset_token = (body or {}).get("resetToken")
    if reset_token:
        results["reset_password"] = req(
            "POST",
            f"{base_auth}/reset-password",
            {"token": reset_token, "newPassword": "ResetPassword3!"},
        )[0]

    status, body = req(
        "POST",
        f"{base_auth}/login",
        {"email": "admin@local.test", "password": "Admin@12345"},
    )
    results["admin_login"] = status
    admin_token = (body or {}).get("accessToken")

    results["admin_users_admin"] = req(
        "GET",
        f"{base_auth}/admin/users",
        None,
        {"Authorization": f"Bearer {admin_token}"},
    )[0]
    results["admin_users_customer"] = req(
        "GET", f"{base_auth}/admin/users", None, headers
    )[0]

    results["logout"] = req("POST", f"{base_auth}/logout", {"refreshToken": refresh2})[
        0
    ]
    results["refresh_revoked"] = req(
        "POST", f"{base_auth}/refresh", {"refreshToken": refresh2}
    )[0]

    return results


if __name__ == "__main__":
    direct = matrix("http://localhost:8081")
    gateway = matrix("http://localhost:8080")
    print("DIRECT", direct)
    print("GATEWAY", gateway)
