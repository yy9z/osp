import { uploadFile } from '@/api/request'

export async function uploadToOSS(file) {
  const res = await uploadFile(file)
  let url = res.data
  if (typeof url === 'object' && url !== null) {
    url = url.url || url.filename || JSON.stringify(url)
  }
  return String(url)
}
