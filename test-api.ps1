$headers = @{ 'X-School-ID' = '1' }

# Create guardian
$g = @{ fullName='Parent API'; primaryPhone='263777000111'; email='parent.api@example.com' } | ConvertTo-Json
$guard = Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/school/guardians' -Headers $headers -Body $g -ContentType 'application/json'
Write-Output '---GUARDIAN---'
$guard | ConvertTo-Json -Depth 4 | Write-Output

# Create student referencing guardian id
$studentBody = @{ firstName='Child'; lastName='API'; dateOfBirth='2012-06-01'; grade='Grade 2'; guardian = @{ id = $guard.id } } | ConvertTo-Json
$stu = Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/school/students' -Headers $headers -Body $studentBody -ContentType 'application/json'
Write-Output '---STUDENT CREATED---'
$stu | ConvertTo-Json -Depth 6 | Write-Output
$studentId = $stu.id

# Fetch with school1
$fetch1 = Invoke-RestMethod -Uri "http://localhost:8080/api/school/students/$studentId" -Headers $headers
Write-Output '---FETCH WITH SCHOOL 1---'
$fetch1 | ConvertTo-Json -Depth 6 | Write-Output

# Fetch with school2
$headers2 = @{ 'X-School-ID' = '2' }
try {
  $fetch2 = Invoke-RestMethod -Uri "http://localhost:8080/api/school/students/$studentId" -Headers $headers2 -ErrorAction Stop
  Write-Output '---FETCH WITH SCHOOL 2---'
  $fetch2 | ConvertTo-Json -Depth 6 | Write-Output
} catch {
  Write-Output '---FETCH WITH SCHOOL 2---'
  Write-Output "ERROR: $($_.Exception.Message)"
}

# Update grade with school1
$upd = @{ grade='Grade 3' } | ConvertTo-Json
$updated = Invoke-RestMethod -Method Put -Uri "http://localhost:8080/api/school/students/$studentId" -Headers $headers -Body $upd -ContentType 'application/json'
Write-Output '---UPDATED WITH SCHOOL 1---'
$updated | ConvertTo-Json -Depth 6 | Write-Output

# Attempt update with school2
try {
  $updated2 = Invoke-RestMethod -Method Put -Uri "http://localhost:8080/api/school/students/$studentId" -Headers $headers2 -Body $upd -ContentType 'application/json' -ErrorAction Stop
  Write-Output '---UPDATED WITH SCHOOL 2---'
  $updated2 | ConvertTo-Json -Depth 6 | Write-Output
} catch {
  Write-Output '---UPDATED WITH SCHOOL 2---'
  Write-Output "ERROR: $($_.Exception.Message)"
}
 
# Delete with school2
try {
  Invoke-RestMethod -Method Delete -Uri "http://localhost:8080/api/school/students/$studentId" -Headers $headers2 -ErrorAction Stop
  Write-Output '---DELETE WITH SCHOOL 2---'
  Write-Output 'SUCCESS'
} catch {
  Write-Output '---DELETE WITH SCHOOL 2---'
  Write-Output "ERROR: $($_.Exception.Message)"
}

# Delete with school1
try {
  Invoke-RestMethod -Method Delete -Uri "http://localhost:8080/api/school/students/$studentId" -Headers $headers -ErrorAction Stop
  Write-Output '---DELETE WITH SCHOOL 1---'
  Write-Output 'SUCCESS'
} catch {
  Write-Output '---DELETE WITH SCHOOL 1---'
  Write-Output "ERROR: $($_.Exception.Message)"
}
