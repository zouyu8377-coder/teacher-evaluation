$body = [System.Text.Encoding]::UTF8.GetBytes('{"username":"admin","password":"admin123"}')
try {
    $r = [System.Net.HttpWebRequest]::Create("http://localhost:8080/api/auth/login")
    $r.Method = "POST"
    $r.ContentType = "application/json"
    $r.ContentLength = $body.Length
    $rs = $r.GetRequestStream()
    $rs.Write($body, 0, $body.Length)
    $rs.Close()
    $resp = $r.GetResponse()
    Write-Host "Status: $($resp.StatusCode)"
} catch {
    Write-Host "Error: $($_.Exception.Message)"
}