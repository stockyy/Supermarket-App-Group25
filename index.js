const express = require("express")
const app = express()
const path = require("path")

// set view engine
app.set("view engine", "ejs")
app.set("views", path.join(__dirname, "views"))

// middleware
app.use(express.static(path.join(__dirname, "public")))
app.use(express.urlencoded({ extended: true }))

// routes
app.get("/", (req, res) => {
  res.render("index")
})

app.get("/customer/login", (req, res) => {
  res.render("customer/login") // done
})

app.get("/customer/register", (req, res) => {
  res.render("customer/register") // done
})

app.get("/customer/product", (req, res) => {
  res.render("customer/product") // done
})

app.get("/customer/product_detail", (req, res) => {
  res.render("customer/product_detail") // done
})

app.get("/customer/basket", (req, res) => {
  res.render("customer/basket") // done
})

app.get("/customer/checkout", (req, res) => {
  res.render("customer/checkout")  // done
})

app.get("/customer/profile", (req, res) => {
  res.render("customer/profile") // done
})

app.get("/warehouse/dashboard", (req, res) => {
  res.render("warehouse/dashboard") // done
})

app.get("/warehouse/picking-list", (req, res) => {
  res.render("warehouse/picking-list") // done
})

app.get("/warehouse/picking-detail", (req, res) => {
  res.render("warehouse/picking-detail") // done
})

app.get("/warehouse/stock", (req, res) => {
  res.render("warehouse/stock") // done
})

app.get("/management/dashboard", (req, res) => {
  res.render("managment/dashboard") // done
})

app.get("/test", (req, res) => {
  res.render("test") 
})


// start server
app.listen(3000, () => {
  console.log("Server running on http://localhost:3000")
})